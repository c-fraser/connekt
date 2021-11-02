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
package io.github.cfraser.connekt.nats

import io.github.cfraser.connekt.api.Transport
import io.nats.client.AuthHandler
import io.nats.client.Connection
import io.nats.client.ConnectionListener
import io.nats.client.ErrorListener
import io.nats.client.Nats
import io.nats.client.Options
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

/**
 * [NatsTransport] is a [Transport] implementation that uses [NATS](https://nats.io/) to send and
 * receive messages.
 *
 * > [NatsTransport] is *thread-safe* thus access from multiple concurrent threads is allowed.
 *
 * @property connection the [Connection] to use to send and receive messages
 */
class NatsTransport private constructor(private val connection: Connection) : Transport.Base() {

  /**
   * *Subscribe* to the [queue] then *receive* messages.
   *
   * *Unsubscribe* from the [queue] when the [io.github.cfraser.connekt.api.ReceiveChannel]
   * coroutine is cancelled.
   *
   * @param queue the *queue* to receive messages from
   * @return the [Flow] of [ByteArray]
   */
  override fun CoroutineScope.receive(queue: String): Flow<ByteArray> {
    val subscription = connection.subscribe(queue, queue)
    coroutineContext[Job]?.invokeOnCompletion {
      if (subscription.isActive) subscription.unsubscribe()
    }
    return flow {
      while (isActive && subscription.isActive) {
        val message = runInterruptible(Dispatchers.IO) { subscription.nextMessage(Duration.ZERO) }
        message?.data?.also { bytes -> emit(bytes) }
      }
    }
  }

  /**
   * *Send* the [byteArray] to the the [queue] using the [connection].
   *
   * @param queue the *queue* to send the message to
   * @param byteArray the [ByteArray] to send
   */
  override suspend fun send(queue: String, byteArray: ByteArray) {
    withContext(Dispatchers.IO) { connection.publish(queue, byteArray) }
  }

  override fun close() {
    connection.close()
  }

  /**
   * Use the [NatsTransport.Builder] class to [build] a [NatsTransport] with the
   * [builder pattern](https://en.wikipedia.org/wiki/Builder_pattern).
   */
  class Builder : Transport.Builder {

    private val optionsBuilder = Options.Builder()

    /**
     * Connect to the *NATS* server with the [serverURL].
     *
     * @param serverURL the URL of the *NATS* server
     * @return the [NatsTransport.Builder]
     */
    fun serverURL(serverURL: String) = apply { optionsBuilder.server(serverURL) }

    /**
     * Configure the timeout for connection attempts.
     *
     * @param connectionTimeout the connection timeout [Duration]
     * @return the [NatsTransport.Builder]
     */
    fun connectionTimeout(connectionTimeout: Duration) = apply {
      optionsBuilder.connectionTimeout(connectionTimeout)
    }

    /**
     * Configure the interval for *ping* attempts.
     *
     * @param pingInterval the ping interval [Duration]
     * @return the [NatsTransport.Builder]
     */
    fun pingInterval(pingInterval: Duration) = apply { optionsBuilder.pingInterval(pingInterval) }

    /**
     * Configure the time to wait between reconnect attempts.
     *
     * @param reconnectWait the re-connect wait [Duration]
     * @return the [NatsTransport.Builder]
     */
    fun reconnectWait(reconnectWait: Duration) = apply {
      optionsBuilder.reconnectWait(reconnectWait)
    }

    /**
     * Receive error events asynchronously with the [errorListener].
     *
     * @param errorListener the [ErrorListener] to use to react to error events
     * @return the [NatsTransport.Builder]
     */
    fun errorListener(errorListener: ErrorListener) = apply {
      optionsBuilder.errorListener(errorListener)
    }

    /**
     * Receive connection events asynchronously with the [connectionListener].
     *
     * @param connectionListener the [ConnectionListener] to use to react to connection events
     * @return the [NatsTransport.Builder]
     */
    fun connectionListener(connectionListener: ConnectionListener) = apply {
      optionsBuilder.connectionListener(connectionListener)
    }

    /**
     * Handle authentication with the [authHandler].
     *
     * @param authHandler the [AuthHandler] to use to authenticate
     * @return the [NatsTransport.Builder]
     */
    fun authHandler(authHandler: AuthHandler) = apply { optionsBuilder.authHandler(authHandler) }

    /**
     * Build the [NatsTransport].
     *
     * @return the [Transport]
     */
    override fun build(): Transport {
      val options = optionsBuilder.build()
      return NatsTransport(Nats.connect(options))
    }
  }
}
