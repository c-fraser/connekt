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
import com.github.michaelbull.retry.policy.binaryExponentialBackoff
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import io.github.cfraser.connekt.api.ExperimentalTransport
import io.github.cfraser.connekt.api.Metrics
import io.github.cfraser.connekt.api.ReceiveChannel
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
import io.rsocket.core.RSocketClient
import io.rsocket.core.RSocketConnector
import io.rsocket.core.RSocketServer
import io.rsocket.frame.FrameType
import io.rsocket.loadbalance.LoadbalanceRSocketClient
import io.rsocket.loadbalance.LoadbalanceTarget
import io.rsocket.metadata.AuthMetadataCodec
import io.rsocket.metadata.RoutingMetadata
import io.rsocket.metadata.TaggingMetadataCodec
import io.rsocket.metadata.WellKnownAuthType
import io.rsocket.micrometer.MicrometerDuplexConnectionInterceptor
import io.rsocket.micrometer.MicrometerRSocketInterceptor
import io.rsocket.util.ByteBufPayload
import java.io.Closeable
import java.time.Duration
import java.util.UUID
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType

/**
 * [RSocketTransport] is a [Transport] implementation that uses [RSocketClient] and [RSocketServer]
 * to, respectively, send and receive messages.
 *
 * [RSocketTransport] is an [ExperimentalTransport] because [RSocket](https://rsocket.io/) is a
 * fundamentally different *messaging technology* relative to the other supported *connekt*
 * transports. *RSocket* is simply an application protocol enabling
 * [Reactive Streams](https://www.reactive-streams.org/) semantics for peer-to-peer communication.
 * Typically, a [Transport] will leverage a *messaging system* with a
 * [message broker](https://en.wikipedia.org/wiki/Message_broker), which often significantly
 * simplifies the design and implementation.
 *
 * > [RSocketTransport] is *thread-safe* thus access from multiple concurrent threads is allowed.
 *
 * @property queueDestinationResolver the [QueueDestinationResolver] to use to determine the
 * destination(s) of a *queue*
 * @property token the authorized token required to establish a [RSocketClient] connection
 * @property serverTransportInitializer the [ServerTransportInitializer] to use to initialize the
 * [io.rsocket.transport.ServerTransport] used by the [RSocketServer]
 * @property clientTransportInitializer the [ClientTransportInitializer] to use to initialize the
 * [io.rsocket.transport.ClientTransport] used by the [RSocketClient] instances
 * @property meterRegistry the [MeterRegistry] to use to track [RSocketServer] and [RSocketClient]
 * metrics
 */
@ExperimentalTransport
class RSocketTransport
private constructor(
    private val queueDestinationResolver: QueueDestinationResolver,
    private val token: String,
    private val serverTransportInitializer: ServerTransportInitializer,
    private val clientTransportInitializer: ClientTransportInitializer,
    private val meterRegistry: MeterRegistry,
) : Transport.Base() {

  /**
   * The [Tag] for this [RSocketTransport] which allows for the relevant metrics to be retrieved
   * from the [meterRegistry].
   */
  private val tag by lazy { Tag.of("rsocket-transport", UUID.randomUUID().toString().take(5)) }

  /**
   * The [SharedRSocketServer] which initializes the [RSocketServer] and distributes the received
   * [Payload] data.
   */
  private val sharedRSocketServer: SharedRSocketServer by lazy {
    SharedRSocketServer(meterRegistry, token, serverTransportInitializer, tag)
  }

  /**
   * The [AsyncLoadingCache] which initializes and stores the [RSocketClient] instances for the
   * *queue* destination(s).
   *
   * The destination IP socket address(es) are re-resolved periodically so that queue destination
   * changes are reacted to in a relatively timely manner.
   */
  private val rSocketClientCache: AsyncLoadingCache<String, RSocketClient> by lazy {
    Caffeine.newBuilder().buildAsync { queue, executor ->
      GlobalScope.future(executor.asCoroutineDispatcher()) {
        val loadBalanceTargets = flow {
          while (true) {
            val destinations =
                queueDestinationResolver
                    .resolve(queue)
                    .apply {
                      logger.debug {
                        "Resolved destination address(es) ${joinToString()} for queue $queue"
                      }
                    }
                    .map { address ->
                      // Client transport for connecting to server
                      val clientTransport = clientTransportInitializer(address)
                      LoadbalanceTarget.from("$address", clientTransport)
                    }
            emit(destinations)

            delay(Duration.ofMinutes(1).toMillis())
          }
        }

        // Connector specifying rSocket connection semantics
        val connector =
            RSocketConnector.create()
                // Track requester metrics using the interceptor and meter registry
                .interceptors { interceptorRegistry ->
                  interceptorRegistry.forRequester(MicrometerRSocketInterceptor(meterRegistry, tag))
                }
                // Initialize setup payload with auth token
                .setupPayload(createSetupPayload(token))

        LoadbalanceRSocketClient.builder(loadBalanceTargets.asPublisher(Dispatchers.IO))
            .connector(connector)
            .roundRobinLoadbalanceStrategy()
            .build()
      }
    }
  }

  /**
   * *Receive* messages from the [sharedRSocketServer] and emit [Payload] data that was routed to
   * the [queue].
   *
   * @param queue the *queue* to receive messages from
   * @return the [Flow] of [ByteArray]
   */
  override fun CoroutineScope.receive(queue: String): Flow<ByteArray> {
    return sharedRSocketServer
        .payloads
        // Defensively copy the payload since it could be emitted to multiple receivers
        .map { payload -> payload.copy() }
        // Only emit payloads that were routed to the queue
        .filter { payload ->
          payload
              .runCatching { decodeRoutingMetadata() == queue }
              .onFailure { logger.error(it) { "Failed to decode routing metadata" } }
              .getOrDefault(false)
        }
        .map { payload -> withContext(Dispatchers.Default) { payload.sliceData().array() } }
  }

  /**
   * *Send* the [byteArray] to the appropriate destination via the [RSocketClient] instances in the
   * [rSocketClientCache] for the [queue].
   *
   * @param queue the *queue* to send the message to
   * @param byteArray the [ByteArray] to send
   */
  override suspend fun send(queue: String, byteArray: ByteArray) {
    val payload = mono { createPayload(byteArray, queue) }
    rSocketClientCache[queue]?.run {
      await()
          .runCatching {
            retry(limitAttempts(10) + binaryExponentialBackoff(500L..60_000L)) {
              fireAndForget(payload).awaitSingleOrNull()
            }
          }
          .onSuccess { logger.debug { "Sent ${byteArray.size} bytes to queue $queue" } }
          .onFailure { logger.error(it) { "Failed to send payload to destination" } }
    }
  }

  override fun metrics() =
      Metrics(
          meterRegistry.count(
              "rsocket.frame", Tags.of(tag).and("frame.type", FrameType.REQUEST_FNF.name)),
          meterRegistry.count(
              "rsocket.request.fnf", Tags.of(tag).and("signal.type", SignalType.ON_COMPLETE.name)),
          meterRegistry.count(
              "rsocket.frame", Tags.of(tag).and("frame.type", FrameType.ERROR.name)),
          meterRegistry.count(
              "rsocket.request.fnf", Tags.of(tag).and("signal.type", SignalType.ON_ERROR.name)))

  /** Close the [sharedRSocketServer] and [RSocketClient] instances in [rSocketClientCache]. */
  override fun close() {
    sharedRSocketServer.close()
    rSocketClientCache.synchronous().invalidateAll()
  }

  /**
   * Use the [RSocketTransport.Builder] class to [build] a [RSocketTransport] with the
   * [builder pattern](https://en.wikipedia.org/wiki/Builder_pattern).
   */
  class Builder : Transport.Builder {

    private var queueDestinationResolver: QueueDestinationResolver by Delegates.notNull()
    private var token: String = RSocketTransport::class.qualifiedName!!
    private var serverTransportInitializer: ServerTransportInitializer =
        ServerTransportInitializer.DEFAULT
    private var clientTransportInitializer: ClientTransportInitializer =
        ClientTransportInitializer.DEFAULT
    private var meterRegistry: MeterRegistry = SimpleMeterRegistry()

    /**
     * Use the [queueDestinationResolver] to use to determine the destination(s) of a *queue*.
     *
     * @param queueDestinationResolver the [QueueDestinationResolver]
     * @return the [RSocketTransport.Builder]
     */
    fun queueDestinationResolver(queueDestinationResolver: QueueDestinationResolver) = apply {
      this.queueDestinationResolver = queueDestinationResolver
    }

    /**
     * Use the authorized [token] to establish a [RSocket] connection.
     *
     * @param token the required authorization token
     * @return the [RSocketTransport.Builder]
     */
    fun token(token: String) = apply { this.token = token }

    /**
     * Use the [serverTransportInitializer] to use to initialize the
     * [io.rsocket.transport.ServerTransport] used by the [RSocketServer].
     *
     * @param serverTransportInitializer the [ServerTransportInitializer]
     * @return the [RSocketTransport.Builder]
     */
    fun serverTransportInitializer(serverTransportInitializer: ServerTransportInitializer) = apply {
      this.serverTransportInitializer = serverTransportInitializer
    }

    /**
     * Use the [clientTransportInitializer] to use to initialize the
     * [io.rsocket.transport.ClientTransport] used by the [RSocketClient] instances.
     *
     * @param clientTransportInitializer the [ClientTransportInitializer]
     * @return the [RSocketTransport.Builder]
     */
    fun clientTransportInitializer(clientTransportInitializer: ClientTransportInitializer) = apply {
      this.clientTransportInitializer = clientTransportInitializer
    }

    /**
     * Use the [meterRegistry] to use to track [RSocketServer] and [RSocketClient] metrics.
     *
     * @param meterRegistry the [MeterRegistry]
     * @return the [RSocketTransport.Builder]
     */
    fun meterRegistry(meterRegistry: MeterRegistry) = apply { this.meterRegistry = meterRegistry }

    /**
     * Build the [RSocketTransport].
     *
     * @return the [Transport]
     */
    override fun build(): Transport {
      return RSocketTransport(
          queueDestinationResolver,
          token,
          serverTransportInitializer,
          clientTransportInitializer,
          meterRegistry)
    }
  }

  /**
   * [SharedRSocketServer] provides access to a [payloads] received by a [RSocketServer].
   * [SharedRSocketServer] consolidates the active messaging connections and is intended to improve
   * resource utilization by initializing a single [RSocketServer] for all [ReceiveChannel]
   * instances.
   *
   * [SharedRSocketServer], and its usage of [SharedFlow], functions similarly to a
   * [demultiplexer](https://en.wikipedia.org/wiki/Multiplexer). [Payload] data is received by the
   * [rSocketServer] and emitted to the [mutableSharedFlow], then [ReceiveChannel] instances receive
   * messages by *subscribing* to the [payloads] and filtering only relevant data (by checking the
   * routing metadata).
   */
  private class SharedRSocketServer(
      meterRegistry: MeterRegistry,
      token: String,
      serverTransportInitializer: ServerTransportInitializer,
      tag: Tag
  ) : Closeable {

    private val mutableSharedFlow: MutableSharedFlow<Payload> =
        MutableSharedFlow(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.SUSPEND)

    /** The [Disposable] representing the running resources of the [RSocketServer]. */
    private val rSocketServer: Disposable

    init {
      // Socket connection acceptor that defines server semantics
      val acceptor = SocketAcceptor { setupPayload, _ ->
        mono {
          setupPayload.check(token)

          // RSocket handling the incoming requests
          object : RSocket {
            override fun fireAndForget(payload: Payload): Mono<Void> =
                mono { mutableSharedFlow.emit(payload) }.flatMap { Mono.empty() }
          }
        }
      }

      // Server transport used by RSocketServer
      val serverTransport = serverTransportInitializer()

      rSocketServer =
          RSocketServer.create(acceptor)
              // Track connection metrics using the interceptor and the meter registry
              .interceptors { interceptorRegistry ->
                interceptorRegistry.forConnection(
                    MicrometerDuplexConnectionInterceptor(meterRegistry, tag))
              }
              .bindNow(serverTransport)
    }

    val payloads: SharedFlow<Payload> = mutableSharedFlow.asSharedFlow()

    override fun close() {
      if (!rSocketServer.isDisposed) rSocketServer.dispose()
    }
  }

  private companion object {

    /**
     * Create a [Payload], for the setup connection, that contains the [token] in the auth metadata.
     *
     * @param token the authorized bearer token
     * @return the [Payload]
     */
    suspend fun createSetupPayload(token: String): Payload =
        withContext(Dispatchers.Default) {
          val authMetadata =
              AuthMetadataCodec.encodeBearerMetadata(ByteBufAllocator.DEFAULT, token.toCharArray())
          ByteBufPayload.create(Unpooled.EMPTY_BUFFER, authMetadata)
        }

    /**
     * Create a [Payload] with the [byteArray] data and [RoutingMetadata].
     *
     * @param byteArray the payload
     * @param name the destination *queue* to include in routing metadata
     * @return the [Payload]
     */
    suspend fun createPayload(byteArray: ByteArray, name: String): Payload =
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
    suspend fun ConnectionSetupPayload.check(token: String) {
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
    suspend fun Payload.copy(): Payload =
        withContext(Dispatchers.Default) { ByteBufPayload.create(this@copy) }

    /**
     * Create the [RoutingMetadata] containing the destination [queue].
     *
     * @param queue the destination *queue*
     * @return the [RoutingMetadata]
     */
    suspend fun createRoutingMetadata(queue: String): RoutingMetadata =
        withContext(Dispatchers.Default) {
          TaggingMetadataCodec.createRoutingMetadata(ByteBufAllocator.DEFAULT, queue.chunked(255))
        }

    /**
     * Decode the [RoutingMetadata] from the receiver [Payload] and return the destination *queue*.
     *
     * @return the destination *queue*
     */
    suspend fun Payload.decodeRoutingMetadata(): String =
        withContext(Dispatchers.Default) {
          with(RoutingMetadata(Unpooled.wrappedBuffer(metadata))) { joinToString("") }
        }

    /**
     * Return the [Counter.count] for the metric corresponding to the [metricName] and [tags].
     *
     * @param metricName the name of the metric
     * @param tags the [Tags] on the metric
     */
    fun MeterRegistry.count(metricName: String, tags: Tags) =
        runCatching { get(metricName).tags(tags).counter() }
            .onFailure { logger.warn(it) { "Unable to find counter with name $metricName" } }
            .getOrNull()
            ?.count()
            ?.toLong()
            ?: -1L

    val logger by lazy { KotlinLogging.logger {} }
  }
}
