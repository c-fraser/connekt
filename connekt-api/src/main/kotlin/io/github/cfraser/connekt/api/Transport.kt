/*
Copyright 2021 c-fraser

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package io.github.cfraser.connekt.api

import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel as KotlinxCoroutinesReceiveChannel
import kotlinx.coroutines.channels.SendChannel as KotlinxCoroutinesSendChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mu.KotlinLogging

/**
 * [Transport] provides message sending and receiving capabilities using a message queuing
 * technology.
 *
 * Messages are sent to a *queue* using the [SendChannel] returned by [sendTo], and messages are
 * received from a *queue* using the [ReceiveChannel] returned by [receiveFrom].
 */
interface Transport : Closeable {

  /**
   * Initialize a [ReceiveChannel] to [ReceiveChannel.receive] messages from the [queue].
   *
   * @param queue the *queue* to receive messages from
   * @return the [ReceiveChannel]
   */
  fun receiveFrom(queue: String): ReceiveChannel<ByteArray>

  /**
   * Initialize a [ReceiveChannel] to [ReceiveChannel.receive] *deserialized* messages from the
   * [queue].
   *
   * @param T the type to convert the received bytes to
   * @param queue the *queue* to receive messages from
   * @param deserializer the [Deserializer] to use to deserialize the received messages
   * @return the [ReceiveChannel]
   */
  fun <T> receiveFrom(queue: String, deserializer: Deserializer<T>): ReceiveChannel<T>

  /**
   * Initialize a [SendChannel] to [SendChannel.send] messages to the [queue].
   *
   * @param queue the *queue* to send messages to
   * @return the [SendChannel]
   */
  fun sendTo(queue: String): SendChannel<ByteArray>

  /**
   * Initialize a [SendChannel] to [SendChannel.send] *serialized* messages to the [queue].
   *
   * @param T the type to convert to bytes
   * @param queue the *queue* to send messages to
   * @param serializer the [Serializer] to use to serialize the messages to send
   * @return the [SendChannel]
   */
  fun <T> sendTo(queue: String, serializer: Serializer<T>): SendChannel<T>

  /**
   * Get the [Metrics] for the [Transport].
   *
   * @return the [Metrics]
   */
  fun metrics(): Metrics

  /**
   * [Transport.Base] is an *internal* abstract base class for [Transport] implementations.
   *
   * The class implements [receiveFrom] and [sendTo] which initialize and cache each *distributed
   * channel* for the source/destination *queue*.
   *
   * A default [Metrics] tracking is also provided through the *atomic* counter properties
   * [messagesReceived], [messagesSent], [receiveErrors], and [sendErrors].
   */
  @InternalConnektApi
  abstract class Base : Transport {

    /** The total number of messages received by this [Transport]. */
    private val messagesReceived = atomic(0L)

    /** The total number of messages sent by this [Transport]. */
    private val messagesSent = atomic(0L)

    /** The total number of receive errors for the [Transport]. */
    private val receiveErrors = atomic(0L)

    /** The total number of send errors for the [Transport]. */
    private val sendErrors = atomic(0L)

    /**
     * The [receiveChannels] [MutableMap] is a [ConcurrentHashMap] of *queue* to [ReceiveChannel].
     */
    private val receiveChannels: MutableMap<String, ReceiveChannel<ByteArray>> = ConcurrentHashMap()

    /** The [sendChannels] [MutableMap] is a [ConcurrentHashMap] of *queue* to [SendChannel]. */
    private val sendChannels: MutableMap<String, SendChannel<ByteArray>> = ConcurrentHashMap()

    /**
     * *Receive* the messages from the [queue].
     *
     * @param queue the *queue* to receive messages from
     * @return the [Flow] of [ByteArray]
     */
    abstract fun CoroutineScope.receive(queue: String): Flow<ByteArray>

    /**
     * *Send* the message to the [queue].
     *
     * @param queue the *queue* to send the message to
     * @param byteArray the [ByteArray] to send
     */
    abstract suspend fun send(queue: String, byteArray: ByteArray)

    /**
     * Return a [ReceiveChannel] for the [queue].
     *
     * Subsequent invocations of [receiveFrom] for a *queue* return the [ReceiveChannel] from the
     * [receiveChannels] cache.
     */
    override fun receiveFrom(queue: String): ReceiveChannel<ByteArray> {
      return receiveChannels.computeIfAbsent(queue, ::initializeReceiveChannel)
    }

    /**
     * Initialize a [ReceiveChannel] to [receiveFrom] the [queue] then [Deserializer.deserialize]
     * the received messages.
     */
    override fun <T> receiveFrom(queue: String, deserializer: Deserializer<T>): ReceiveChannel<T> {
      return receiveFrom(queue) + deserializer
    }

    /**
     * Return a [SendChannel] for the [queue].
     *
     * Subsequent invocations of [sendTo] for a *queue* return the [SendChannel] from the
     * [sendChannels] cache.
     */
    override fun sendTo(queue: String): SendChannel<ByteArray> {
      return sendChannels.computeIfAbsent(queue, ::initializeSendChannel)
    }

    /**
     * Initialize a [SendChannel] to [Serializer.serialize] the messages then [sendTo] the [queue].
     */
    override fun <T> sendTo(queue: String, serializer: Serializer<T>): SendChannel<T> {
      return sendTo(queue) + serializer
    }

    /**
     * Get the [Metrics] for the [Transport] from [messagesReceived], [messagesSent],
     * [receiveErrors], and [sendErrors].
     *
     * @return the [Metrics]
     */
    override fun metrics() =
        Metrics(
            messagesReceived = messagesReceived.value,
            messagesSent = messagesSent.value,
            receiveErrors = receiveErrors.value,
            sendErrors = sendErrors.value)

    /**
     * Initialize a [ReceiveChannel] to [receive] messages from the [queue] in a [GlobalScope]
     * coroutine.
     *
     * @param queue the *queue* to receive messages from
     * @return the [ReceiveChannel]
     */
    private fun initializeReceiveChannel(queue: String): ReceiveChannel<ByteArray> {
      return GlobalScope.produce<ByteArray>(Dispatchers.IO) {
            receive(queue)
                .catch { throwable ->
                  receiveErrors += 1L
                  logger.error(throwable) { "Failed to receive from queue $queue" }
                }
                .onEach { byteArray ->
                  messagesReceived += 1L
                  logger.debug { "Received ${byteArray.size} bytes from queue $queue" }
                }
                .collect { byteArray -> channel.send(byteArray) }
          }
          .asReceiveChannel()
    }

    /**
     * Initialize a [SendChannel] to [send] messages to the [queue] in a [GlobalScope] coroutine.
     *
     * @param queue the *queue* to send messages to
     * @return the [SendChannel]
     */
    private fun initializeSendChannel(queue: String): SendChannel<ByteArray> {
      val channel = Channel<ByteArray>()
      GlobalScope.launch(Dispatchers.IO) {
        for (message in channel) runCatching { send(queue, message) }
            .onFailure { throwable ->
              sendErrors += 1L
              logger.error(throwable) { "Failed to send message to queue $queue" }
            }
            .onSuccess {
              messagesSent += 1L
              logger.debug { "Sent ${message.size} bytes to queue $queue" }
            }
      }
      return channel.asSendChannel()
    }

    private companion object {

      /**
       * Use the [KotlinxCoroutinesReceiveChannel] as a [ReceiveChannel].
       *
       * @return the [ReceiveChannel]
       */
      fun <T> KotlinxCoroutinesReceiveChannel<T>.asReceiveChannel(): ReceiveChannel<T> {
        return object : KotlinxCoroutinesReceiveChannel<T> by this, ReceiveChannel<T> {}
      }

      /**
       * Use the [KotlinxCoroutinesSendChannel] as a [SendChannel].
       *
       * @return the [SendChannel]
       */
      fun <T> KotlinxCoroutinesSendChannel<T>.asSendChannel(): SendChannel<T> {
        return object : KotlinxCoroutinesSendChannel<T> by this, SendChannel<T> {}
      }

      /**
       * Convert the [ReceiveChannel] of [ByteArray] to a [ReceiveChannel] of [T] that uses the
       * [deserializer] to [Deserializer.deserialize] received messages.
       *
       * @param T the type to convert the received bytes to
       * @param deserializer the [Deserializer] to use to deserialize the received messages
       * @return the [ReceiveChannel] of [T]
       */
      operator fun <T> ReceiveChannel<ByteArray>.plus(
          deserializer: Deserializer<T>
      ): ReceiveChannel<T> {
        return run receiveChannel@{
          GlobalScope.produce<T>(Dispatchers.Default) {
                for (byteArray in this@receiveChannel) deserializer
                    .runCatching { deserialize(byteArray) }
                    .onFailure { throwable ->
                      logger.error(throwable) { "Failed to deserialize message" }
                    }
                    .onSuccess { message -> channel.send(message) }
              }
              .asReceiveChannel()
        }
      }

      /**
       * Convert the [SendChannel] of [ByteArray] to a [SendChannel] of [T] that uses the
       * [serializer] to [Serializer.serialize] sent messages.
       *
       * @param T the type to convert to bytes
       * @param serializer the [Serializer] to use to serialize the messages to send
       * @return the [SendChannel] of [T]
       */
      operator fun <T> SendChannel<ByteArray>.plus(serializer: Serializer<T>): SendChannel<T> {
        val channel = Channel<T>()
        GlobalScope.launch(Dispatchers.IO) {
          for (message in channel) serializer
              .runCatching { serialize(message) }
              .onFailure { throwable -> logger.error(throwable) { "Failed to serialize message" } }
              .onSuccess { byteArray -> send(byteArray) }
        }
        return channel.asSendChannel()
      }

      val logger = KotlinLogging.logger {}
    }
  }

  /**
   * A [Transport.Builder] *builds* a [Transport] using the
   * [builder pattern](https://en.wikipedia.org/wiki/Builder_pattern).
   */
  @InternalConnektApi
  interface Builder {

    /** Build the [Transport]. */
    fun build(): Transport
  }
}
