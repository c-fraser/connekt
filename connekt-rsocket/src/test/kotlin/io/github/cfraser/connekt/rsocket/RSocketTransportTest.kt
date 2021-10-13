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

import io.github.cfraser.connekt.api.ExperimentalTransport
import io.github.cfraser.connekt.api.Transport
import io.github.cfraser.connekt.test.test
import io.rsocket.transport.local.LocalClientTransport
import io.rsocket.transport.local.LocalServerTransport
import java.net.InetSocketAddress
import kotlin.test.Test

class RSocketTransportTest {

  @Test
  fun testRSocketTransport() {
    localRSocketTransport().test()
  }

  companion object {

    @OptIn(ExperimentalTransport::class)
    fun localRSocketTransport(): Transport {
      val queueDestinationResolver = QueueDestinationResolver { setOf(InetSocketAddress(0)) }
      val (serverTransportInitializer, clientTransportInitializer) =
          RSocketTransportTest::class.simpleName!!.let { name ->
            ServerTransportInitializer { LocalServerTransport.create(name) } to
                ClientTransportInitializer { LocalClientTransport.create(name) }
          }
      return RSocketTransport.Builder()
          .queueDestinationResolver(queueDestinationResolver)
          .serverTransportInitializer(serverTransportInitializer)
          .clientTransportInitializer(clientTransportInitializer)
          .build()
    }
  }
}
