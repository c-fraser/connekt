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

import io.rsocket.Closeable
import io.rsocket.transport.ServerTransport
import io.rsocket.transport.netty.server.TcpServerTransport

/**
 * The [ServerTransportInitializer] type represents a function that initializes a [ServerTransport].
 */
fun interface ServerTransportInitializer {

  /**
   * Initialize a [ServerTransport].
   *
   * @return the [ServerTransport]
   */
  operator fun invoke(): ServerTransport<out Closeable>

  companion object {

    /**
     * The default [ServerTransportInitializer] which creates a [TcpServerTransport] that binds to
     * `localhost` on port `8787`.
     */
    @JvmStatic val DEFAULT = ServerTransportInitializer { TcpServerTransport.create(8787) }
  }
}
