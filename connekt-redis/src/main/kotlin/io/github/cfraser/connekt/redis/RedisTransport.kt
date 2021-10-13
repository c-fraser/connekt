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
package io.github.cfraser.connekt.redis

import io.github.cfraser.connekt.api.Transport
import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.ToByteBufEncoder
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.reactive.ChannelMessage
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import java.io.Closeable
import java.nio.ByteBuffer
import java.time.Duration
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

/**
 * [RedisTransport] is a [Transport] implementation that uses
 * [Redis Pub/Sub](https://redis.io/topics/pubsub) to send and receive messages.
 *
 * > [RedisTransport] is *thread-safe* thus access from multiple concurrent threads is allowed.
 *
 * @param redisClient the [RedisClient] to use to establish pub/sub connections to a *Redis* server.
 */
class RedisTransport private constructor(redisClient: RedisClient) : Transport.Base() {

  /**
   * The [SharedFlow] which receives [ChannelMessage] data for all
   * [io.github.cfraser.connekt.api.ReceiveChannel] instances through the [ChannelMessageListener]
   * which is added to the [receiveConnection].
   */
  private val receivedChannelMessages: SharedFlow<ChannelMessage<String, ByteArray>>

  /** The [PubSubConnection] for *subscribe* commands. */
  private val receiveConnection: PubSubConnection

  /** The [PubSubConnection] for *publish* commands. */
  private val sendConnection: PubSubConnection

  init {
    val mutableSharedFlow = MutableSharedFlow<ChannelMessage<String, ByteArray>>()
    receivedChannelMessages = mutableSharedFlow.asSharedFlow()
    receiveConnection =
        PubSubConnection(redisClient).apply {
          commands.apply {
            statefulConnection.addListener(ChannelMessageListener(mutableSharedFlow))
          }
        }
    sendConnection = PubSubConnection(redisClient)
  }

  /**
   * *Subscribe* to the [queue] using the [receiveConnection], then *receive* messages by filtering
   * the [receivedChannelMessages].
   *
   * *Unsubscribe* from the [queue] when the [io.github.cfraser.connekt.api.ReceiveChannel]
   * coroutine is cancelled.
   *
   * @param queue the *queue* to receive messages from
   * @return the [Flow] of [ByteArray]
   */
  override fun CoroutineScope.receive(queue: String): Flow<ByteArray> {
    runBlocking(Dispatchers.IO) { receiveConnection.commands.subscribe(queue).awaitSingleOrNull() }
    coroutineContext[Job]?.invokeOnCompletion {
      receiveConnection.commands.unsubscribe(queue).block(Duration.ofSeconds(10))
    }
    return receivedChannelMessages
        .filter { channelMessage -> channelMessage.channel == queue }
        .mapNotNull { channelMessage -> channelMessage.message }
  }

  /**
   * *Send* the [byteArray] to the the [queue] using the [sendConnection].
   *
   * @param queue the *queue* to send the message to
   * @param byteArray the [ByteArray] to send
   */
  override suspend fun send(queue: String, byteArray: ByteArray) {
    sendConnection.commands.publish(queue, byteArray).awaitSingleOrNull()
  }

  override fun close() {
    for (closeable in listOf<Closeable>(receiveConnection, sendConnection)) {
      closeable.runCatching { close() }.onFailure {
        logger.warn(it) { "Failed to close connection" }
      }
    }
  }

  /**
   * Use the [RedisTransport.Builder] class to [build] a [RedisTransport] with the
   * [builder pattern](https://en.wikipedia.org/wiki/Builder_pattern).
   */
  class Builder : Transport.Builder {

    private var redisURI: RedisURI by Delegates.notNull()
    private var clientResources: ClientResources = DefaultClientResources.create()
    private var clientOptions: ClientOptions = ClientOptions.create()

    /**
     * Connect to the *Redis* server with the [redisURI].
     *
     * @param redisURI the [RedisURI] of the *Redis* server
     * @return the [RedisTransport.Builder]
     */
    fun redisURI(redisURI: RedisURI) = apply { this.redisURI = redisURI }

    /**
     * Specify the [clientResources] for the [RedisClient].
     *
     * @param clientResources the [ClientResources] configuration
     * @return the [RedisTransport.Builder]
     */
    fun clientResources(clientResources: ClientResources) = apply {
      this.clientResources = clientResources
    }

    /**
     * Specify the [clientOptions] for the [RedisClient].
     *
     * @param clientOptions the [ClientOptions] configuration
     * @return the [RedisTransport.Builder]
     */
    fun clientOptions(clientOptions: ClientOptions) = apply { this.clientOptions = clientOptions }

    /**
     * Build the [RedisTransport].
     *
     * @return the [Transport]
     */
    override fun build(): Transport {
      val redisClient = RedisClient.create(clientResources, redisURI)
      redisClient.options = clientOptions
      return RedisTransport(redisClient)
    }
  }

  /**
   * [PubSubConnection] consolidates the *pub-sub* [connection] and [commands] for a *Redis* server.
   *
   * @param redisClient the [RedisClient] to use to connect the *Redis* server
   */
  private class PubSubConnection(redisClient: RedisClient) : Closeable {

    /** The [StatefulRedisPubSubConnection] to the *Redis* server. */
    val connection: StatefulRedisPubSubConnection<String, ByteArray> =
        redisClient.connectPubSub(Codec)

    /** The [RedisPubSubReactiveCommands] to use to interact with the *Redis* server. */
    val commands: RedisPubSubReactiveCommands<String, ByteArray> = connection.reactive()

    override fun close() {
      if (connection.isOpen) connection.close()
    }
  }

  /**
   * An optimized [RedisCodec] that encodes and decodes [Charsets.UTF_8] [String] keys and
   * [ByteArray] values.
   */
  private object Codec : RedisCodec<String, ByteArray>, ToByteBufEncoder<String, ByteArray> {

    private val empty = ByteArray(0)
    private val charset = Charsets.UTF_8

    override fun decodeKey(bytes: ByteBuffer?): String {
      return Unpooled.wrappedBuffer(bytes ?: ByteBuffer.wrap(empty)).toString(charset)
    }

    override fun decodeValue(bytes: ByteBuffer?): ByteArray {
      if (bytes == null) return empty
      val remaining = bytes.remaining()
      return if (remaining == 0) empty
      else ByteArray(remaining).also { byteArray -> bytes.get(byteArray) }
    }

    override fun encodeKey(key: String?): ByteBuffer {
      if (key == null) return ByteBuffer.wrap(empty)
      val buffer = ByteBuffer.allocate(ByteBufUtil.utf8MaxBytes(key))
      val byteBuf = Unpooled.wrappedBuffer(buffer)
      byteBuf.clear()
      ByteBufUtil.writeUtf8(byteBuf, key)
      buffer.limit(byteBuf.writerIndex())
      return buffer
    }

    override fun encodeValue(value: ByteArray?): ByteBuffer {
      return ByteBuffer.wrap(value ?: empty)
    }

    override fun encodeKey(key: String?, target: ByteBuf?) {
      if (key == null || target == null) return
      ByteBufUtil.writeUtf8(target, key)
    }

    override fun encodeValue(value: ByteArray?, target: ByteBuf?) {
      if (value == null || target == null) return
      target.writeBytes(value)
    }

    override fun estimateSize(keyOrValue: Any?): Int {
      return when (keyOrValue) {
        is String -> ByteBufUtil.utf8MaxBytes(keyOrValue)
        is ByteArray -> keyOrValue.size
        else -> 0
      }
    }
  }

  /**
   * [ChannelMessageListener] is a [RedisPubSubListener] that emits *channel messages* to the given
   * [mutableSharedFlow].
   *
   * @property mutableSharedFlow the [MutableSharedFlow] which provides access to received *channel
   * messages*
   */
  private class ChannelMessageListener(
      private val mutableSharedFlow: MutableSharedFlow<ChannelMessage<String, ByteArray>>
  ) : RedisPubSubListener<String, ByteArray> {

    override fun message(channel: String?, message: ByteArray?) {
      if (channel == null || message == null) return
      runBlocking(Dispatchers.Default) { mutableSharedFlow.emit(ChannelMessage(channel, message)) }
    }

    override fun message(pattern: String?, channel: String?, message: ByteArray?) {
      logger.debug { "Received message for channel $channel with pattern $pattern" }
    }

    override fun subscribed(channel: String?, count: Long) {
      logger.debug { "Subscribed to channel $channel" }
    }

    override fun psubscribed(pattern: String?, count: Long) {
      logger.debug { "Subscribed to pattern $pattern" }
    }

    override fun unsubscribed(channel: String?, count: Long) {
      logger.debug { "Unsubscribed from channel $channel" }
    }

    override fun punsubscribed(pattern: String?, count: Long) {
      logger.debug { "Unsubscribed from pattern $pattern" }
    }
  }

  private companion object {

    val logger by lazy { KotlinLogging.logger {} }
  }
}
