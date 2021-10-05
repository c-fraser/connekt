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

import io.github.cfraser.connekt.api.Transport
import io.rsocket.transport.local.LocalClientTransport
import io.rsocket.transport.local.LocalServerTransport
import java.net.InetSocketAddress
import java.time.Duration
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class RSocketTransportTest {

  @Test
  fun testRSocketTransport() {
    localRSocketTransport().use { transport ->
      val numberOfMessages = 1_000
      val sendMessages = (1..numberOfMessages).map { UUID.randomUUID().toString() }
      val numberOfQueues = 5
      val receivedMessages = arrayOfNulls<MutableList<String>>(numberOfQueues)

      runBlocking {
        val jobs = mutableListOf<Job>()

        repeat(numberOfQueues) { i ->
          receivedMessages[i] = mutableListOf()

          val sendChannel = transport.sendTo("$i")
          val receiveChannel = transport.receiveFrom("$i")

          jobs +=
              launch {
                receivedMessages[i]!!.run {
                  for (byteArray in receiveChannel) {
                    this += String(byteArray)
                    if (size == numberOfMessages) cancel()
                  }
                }
              }

          jobs += launch { sendMessages.map { it.toByteArray() }.forEach { sendChannel.send(it) } }
        }

        withTimeout(Duration.ofSeconds(10).toMillis()) { jobs.joinAll() }
      }

      receivedMessages.forEach { assertTrue { it!!.containsAll(sendMessages) } }

      with(transport.metrics()) {
        assertEquals(0L, receiveErrors)
        assertEquals(0L, sendErrors)
      }
    }
  }

  companion object {

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
