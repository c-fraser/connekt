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

import io.github.cfraser.connekt.api.ReceiveChannel.Companion.asReceiveChannel
import io.github.cfraser.connekt.api.SendChannel.Companion.asSendChannel
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
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
 * Messages are asynchronously sent to a *queue* using the [SendChannel] returned by [sendTo], and
 * messages are asynchronously received from a *queue* using the [ReceiveChannel] returned by
 * [receiveFrom].
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
   * Initialize a [SendChannel] to [SendChannel.send] messages to the [queue].
   *
   * @param queue the *queue* to send messages to
   * @return the [SendChannel]
   */
  fun sendTo(queue: String): SendChannel<ByteArray>

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
     * Initialize a [ReceiveChannel] to [receive] messages from the [queue] in a [GlobalScope]
     * coroutine.
     *
     * Subsequent invocations of [receiveFrom] for a *queue* return the [ReceiveChannel] from the
     * [receiveChannels] cache.
     *
     * @param queue the *queue* to receive messages from
     * @return the [ReceiveChannel]
     */
    override fun receiveFrom(queue: String): ReceiveChannel<ByteArray> {
      return receiveChannels.computeIfAbsent(queue) { _queue ->
        GlobalScope.produce<ByteArray>(Dispatchers.IO) {
          receive(_queue)
              .catch { throwable ->
                receiveErrors += 1L
                logger.warn(throwable) { "Failed to receive from queue $_queue" }
              }
              .onEach { byteArray ->
                messagesReceived += 1L
                logger.debug { "Received ${byteArray.size} bytes from queue $_queue" }
              }
              .collect { byteArray -> channel.send(byteArray) }
        }
            .run { asReceiveChannel() }
      }
    }

    /**
     * Initialize a [SendChannel] to [send] messages to the [queue] in a [GlobalScope] coroutine.
     *
     * Subsequent invocations of [sendTo] for a *queue* return the [SendChannel] from the
     * [sendChannels] cache.
     *
     * @param queue the *queue* to send messages to
     * @return the [SendChannel]
     */
    override fun sendTo(queue: String): SendChannel<ByteArray> {
      return sendChannels.computeIfAbsent(queue) { _queue ->
        Channel<ByteArray>()
            .also { channel ->
              GlobalScope.launch(Dispatchers.IO) {
                for (message in channel) runCatching { send(_queue, message) }
                    .onFailure { throwable ->
                      sendErrors += 1L
                      logger.warn(throwable) { "Failed to send message to queue $_queue" }
                    }
                    .onSuccess {
                      messagesSent += 1L
                      logger.debug { "Sent ${message.size} bytes to queue $_queue" }
                    }
              }
            }
            .run { asSendChannel() }
      }
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

    private companion object {

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
