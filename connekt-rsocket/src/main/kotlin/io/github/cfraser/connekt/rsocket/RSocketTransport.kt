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
package io.github.cfraser.connekt.rsocket

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import io.github.cfraser.connekt.api.ReceiveChannel
import io.github.cfraser.connekt.api.ReceiveChannel.Companion.toReceiveChannel
import io.github.cfraser.connekt.api.SendChannel
import io.github.cfraser.connekt.api.SendChannel.Companion.toSendChannel
import io.github.cfraser.connekt.api.Transport
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.rsocket.ConnectionSetupPayload
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.SocketAcceptor
import io.rsocket.core.RSocketConnector
import io.rsocket.core.RSocketServer
import io.rsocket.frame.FrameType
import io.rsocket.metadata.AuthMetadataCodec
import io.rsocket.metadata.RoutingMetadata
import io.rsocket.metadata.TaggingMetadataCodec
import io.rsocket.metadata.WellKnownAuthType
import io.rsocket.micrometer.MicrometerDuplexConnectionInterceptor
import io.rsocket.micrometer.MicrometerRSocketInterceptor
import io.rsocket.util.ByteBufPayload
import java.time.Duration
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import reactor.core.Disposable
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType

/**
 * [RSocketTransport] is a [Transport] implementation that uses [RSocket] and [RSocketServer] to
 * send and receive messages respectively.
 *
 * @property topicDestinationResolver the [TopicDestinationResolver] to use to determine the
 * destination(s) of a *topic*
 * @property token the authorized token required to establish a [RSocket] connection
 * @property serverTransportInitializer the [ServerTransportInitializer] to use to initialize the
 * [io.rsocket.transport.ServerTransport] used by the [RSocketServer]
 * @property clientTransportInitializer the [ClientTransportInitializer] to use to initialize the
 * [io.rsocket.transport.ClientTransport] used by the [RSocket] instances
 */
class RSocketTransport
private constructor(
    private val topicDestinationResolver: TopicDestinationResolver,
    private val token: String,
    private val serverTransportInitializer: ServerTransportInitializer,
    private val clientTransportInitializer: ClientTransportInitializer
) : Transport {

  /**
   * The [Tag] for this [RSocketTransport] which allows for the relevant metrics to be retrieved
   * from the [meterRegistry].
   */
  private val tag by lazy { Tag.of("rsocket-transport", UUID.randomUUID().toString().take(5)) }

  /**
   * The [SharedFlow] for distributing [Payload] data to receive channel(s).
   *
   * The [sharedFlow] is used as a [demultiplexer](https://en.wikipedia.org/wiki/Multiplexer).
   * [Payload] data for all [ReceiveChannel] instances are received by the single running
   * [rSocketServer] and emitted to the [sharedFlow]. [ReceiveChannel] instances receive messages by
   * *subscribing* to the [SharedFlow] and filtering only relevant [Payload] data (by checking the
   * routing metadata).
   */
  private val sharedFlow by lazy { MutableSharedFlow<Payload>() }

  /**
   * The [Disposable] representing the running resources of the [RSocketServer]. This consolidates
   * the active messaging connections and is intended to improve resource utilization.
   */
  private val rSocketServer by lazy {
    // Override on error dropped strategy (https://github.com/rsocket/rsocket-java/issues/1018)
    Hooks.onErrorDropped {}

    // Socket connection acceptor that defines server semantics
    val acceptor = SocketAcceptor { setupPayload, _ ->
      mono {
        setupPayload.check(token)

        // RSocket handling the incoming requests
        object : RSocket {
          override fun fireAndForget(payload: Payload): Mono<Void> =
              mono { sharedFlow.emit(payload) }.flatMap { Mono.empty() }
        }
      }
    }

    // Server transport used by RSocketServer
    val transport = serverTransportInitializer()

    RSocketServer.create(acceptor)
        // Track connection metrics using the interceptor and the meter registry
        .interceptors { interceptorRegistry ->
          interceptorRegistry.forConnection(
              MicrometerDuplexConnectionInterceptor(meterRegistry, tag))
        }
        .bindNow(transport)
  }

  /**
   * The [LoadingCache] which initializes and stores the [ReceiveChannel] for each *topic*.
   *
   * Each [ReceiveChannel] *subscribes* to the [sharedFlow] and emits payload data that was routed
   * to the corresponding *topic*.
   */
  private val receiveChannels by lazy {
    Caffeine.newBuilder().build<String, ReceiveChannel<ByteArray>> { topic ->
      GlobalScope.produce<ByteArray>(Dispatchers.IO) {
        // Defensively copy the payload since it could be emitted to multiple receivers
        sharedFlow
            .map { payload -> payload.copy() }
            // Only emit payloads that were routed to the topic
            .filter { payload ->
              payload.runCatching { decodeRoutingMetadata() == topic }.getOrDefault(false)
            }
            .map { payload -> withContext(Dispatchers.Default) { payload.sliceData().array() } }
            .collect { byteArray -> channel.send(byteArray) }
      }
          .run { toReceiveChannel() }
    }
  }

  /**
   * The [AsyncLoadingCache] which initializes and stores the [RSocket] instances for the *topic*
   * destination(s).
   *
   * The destination IP socket address(es) are only cached temporarily so that topic destination
   * resolution changes are reacted to in a relatively timely manner.
   */
  private val rSocketCache by lazy {
    // The keep-alive (and cache expiration) time for the RSocket connections
    val maxKeepAlive = Duration.ofMinutes(2)

    Caffeine.newBuilder()
        .expireAfterWrite(maxKeepAlive)
        .evictionListener<String, List<RSocket>> { _, rSockets, _ ->
          rSockets?.forEach { rSocket -> runCatching(rSocket::dispose) }
        }
        .buildAsync<String, List<RSocket>> { topic, executor ->
          GlobalScope.future(executor.asCoroutineDispatcher()) {
            topicDestinationResolver
                .resolve(topic)
                .mapNotNull { address ->
                  runCatching {
                        // Connector specifying rSocket connection semantics
                        val connector =
                            RSocketConnector.create()
                                .keepAlive(Duration.ofSeconds(30), maxKeepAlive)
                                // Track requester metrics using the interceptor and meter registry
                                .interceptors { interceptorRegistry ->
                                  interceptorRegistry.forRequester(
                                      MicrometerRSocketInterceptor(meterRegistry, tag))
                                }
                                // Initialize setup payload with auth token
                                .setupPayload(createSetupPayload(token))

                        // Client transport for connecting to server
                        val transport = clientTransportInitializer(address)

                        // Establish a connection to the `address` using the auth `token`
                        connector.connect(transport).awaitSingleOrNull()
                      }
                      .getOrNull()
                }
                .takeUnless { it.isEmpty() }
          }
        }
  }

  /**
   * The [LoadingCache] which initializes and stores the [SendChannel] for each *topic*.
   *
   * The bytes sent to each [SendChannel] are delivered to the appropriate destination via the
   * [RSocket] instances in the [rSocketCache] for the *topic*.
   */
  private val sendChannels by lazy {
    Caffeine.newBuilder().build<String, SendChannel<ByteArray>> { topic ->
      Channel<ByteArray>(Channel.BUFFERED)
          .also { channel ->
            GlobalScope.launch {
              for (byteArray in channel) {
                val payload = createPayload(byteArray, topic)
                rSocketCache[topic]?.run {
                  await()
                      .orEmpty()
                      .map { rSocket ->
                        launch(Dispatchers.IO) {
                          runCatching { rSocket.fireAndForget(payload.copy()).awaitSingleOrNull() }
                        }
                      }
                      .joinAll()
                }
              }
            }
          }
          .run { toSendChannel() }
    }
  }

  override fun receive(topic: String): ReceiveChannel<ByteArray> {
    return receiveChannels[topic] ?: error("Failed to initialize receive channel for topic $topic")
  }

  override fun send(topic: String): SendChannel<ByteArray> {
    return sendChannels[topic] ?: error("Failed to initialize send channel for topic $topic")
  }

  override fun metrics(): Transport.Metrics {
    return object : Transport.Metrics {
      override val messagesReceived =
          count("rsocket.frame", Tags.of(tag).and("frame.type", FrameType.REQUEST_FNF.name))
      override val messagesSent =
          count("rsocket.request.fnf", Tags.of(tag).and("signal.type", SignalType.ON_COMPLETE.name))
      override val receiveErrors =
          count("rsocket.frame", Tags.of(tag).and("frame.type", FrameType.ERROR.name))
      override val sendErrors =
          count("rsocket.request.fnf", Tags.of(tag).and("signal.type", SignalType.ON_ERROR.name))
    }
  }

  /** Close the [RSocketServer] and [RSocket] clients. */
  override fun close() {
    if (!rSocketServer.isDisposed) rSocketServer.dispose()
    rSocketCache.synchronous().invalidateAll()
  }

  companion object {

    /**
     * Factory function to initialize a [RSocketTransport].
     *
     * @param topicDestinationResolver the [TopicDestinationResolver] to use to determine the
     * destination(s) of a *topic*
     * @param token the authorized token required to establish a [RSocket] connection
     * @param serverTransportInitializer the [ServerTransportInitializer] to use to initialize the
     * [io.rsocket.transport.ServerTransport] used by the [RSocketServer]
     * @param clientTransportInitializer the [ClientTransportInitializer] to use to initialize the
     * [io.rsocket.transport.ClientTransport] used by the [RSocket] instances
     * @return the [Transport]
     */
    @JvmOverloads
    @JvmStatic
    fun new(
        topicDestinationResolver: TopicDestinationResolver,
        token: String = RSocketTransport::class.qualifiedName!!,
        serverTransportInitializer: ServerTransportInitializer = ServerTransportInitializer.DEFAULT,
        clientTransportInitializer: ClientTransportInitializer = ClientTransportInitializer.DEFAULT
    ): Transport {
      return RSocketTransport(
          topicDestinationResolver, token, serverTransportInitializer, clientTransportInitializer)
    }

    /** The [MeterRegistry] to use to track [RSocketServer] and [RSocket] metrics. */
    private val meterRegistry by lazy { SimpleMeterRegistry() }

    /**
     * Return the [Counter.count] for the metric corresponding to the [metricName] and [tags].
     *
     * @param metricName the name of the metric
     * @param tags the [Tags] on the metric
     */
    private fun count(metricName: String, tags: Tags) =
        runCatching { meterRegistry.get(metricName).tags(tags).counter() }
            .getOrNull()
            ?.count()
            ?.toLong()
            ?: -1L

    /**
     * Create a [Payload], for the setup connection, that contains the [token] in the auth metadata.
     *
     * @param token the authorized bearer token
     * @return the [Payload]
     */
    private suspend fun createSetupPayload(token: String): Payload =
        withContext(Dispatchers.Default) {
          val authMetadata =
              AuthMetadataCodec.encodeBearerMetadata(ByteBufAllocator.DEFAULT, token.toCharArray())
          ByteBufPayload.create(Unpooled.EMPTY_BUFFER, authMetadata)
        }

    /**
     * Create a [Payload] with the [byteArray] data and [RoutingMetadata].
     *
     * @param byteArray the payload
     * @param name the destination *topic* to include in routing metadata
     * @return the [Payload]
     */
    private suspend fun createPayload(byteArray: ByteArray, name: String): Payload =
        withContext(Dispatchers.Default) {
          ByteBufPayload.create(
              Unpooled.wrappedBuffer(byteArray), createRoutingMetadata(name).content)
        }

    /**
     * Check the receiver [ConnectionSetupPayload].
     *
     * Specifically ensure the [ConnectionSetupPayload.getMetadata] contains the
     * [WellKnownAuthType.BEARER] auth metadata matching the [token].
     *
     * @param token the authorized bearer token
     * @throws [IllegalStateException] if the [ConnectionSetupPayload] is invalid
     */
    private suspend fun ConnectionSetupPayload.check(token: String) {
      withContext(Dispatchers.Default) {
        val byteBuf = Unpooled.wrappedBuffer(metadata)
        check(AuthMetadataCodec.isWellKnownAuthType(byteBuf)) { "unrecognized metadata" }
        AuthMetadataCodec.readWellKnownAuthType(byteBuf).apply {
          check(this == WellKnownAuthType.BEARER) { "unexpected auth type $this" }
        }
        with(String(AuthMetadataCodec.readBearerTokenAsCharArray(byteBuf))) {
          check(this == token) { "received invalid token $this" }
        }
      }
    }

    /**
     * Copy the receiver [Payload].
     *
     * @return the copied payload
     */
    private suspend fun Payload.copy(): Payload =
        withContext(Dispatchers.Default) { ByteBufPayload.create(this@copy) }

    /**
     * Create the [RoutingMetadata] containing the destination [topic].
     *
     * @param topic the destination *topic*
     * @return the [RoutingMetadata]
     */
    private suspend fun createRoutingMetadata(topic: String): RoutingMetadata =
        withContext(Dispatchers.Default) {
          TaggingMetadataCodec.createRoutingMetadata(ByteBufAllocator.DEFAULT, topic.chunked(255))
        }

    /**
     * Decode the [RoutingMetadata] from the receiver [Payload] and return the destination *topic*.
     *
     * @return the destination *topic*
     */
    private suspend fun Payload.decodeRoutingMetadata(): String =
        withContext(Dispatchers.Default) {
          with(RoutingMetadata(Unpooled.wrappedBuffer(metadata))) { joinToString("") }
        }
  }
}
