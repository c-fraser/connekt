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

import io.rsocket.transport.ClientTransport
import io.rsocket.transport.netty.client.TcpClientTransport
import java.net.InetSocketAddress

/**
 * The [ClientTransportInitializer] type represents a function that initializes a [ClientTransport].
 */
fun interface ClientTransportInitializer {

  /**
   * Initialize a [ClientTransport] to connect to the [inetSocketAddress].
   *
   * @param inetSocketAddress the [InetSocketAddress] to connect to
   * @return the [ClientTransport]
   */
  operator fun invoke(inetSocketAddress: InetSocketAddress): ClientTransport

  companion object {

    /**
     * The default [ClientTransportInitializer] which creates a [TcpClientTransport] connecting to
     * the given [InetSocketAddress].
     */
    @JvmStatic
    val DEFAULT = ClientTransportInitializer { address -> TcpClientTransport.create(address) }
  }
}
