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
package io.github.cfraser.connekt.test

import io.github.cfraser.connekt.api.Metrics
import io.github.cfraser.connekt.api.ReceiveChannel
import io.github.cfraser.connekt.api.ReceiveChannel.Companion.asReceiveChannel
import io.github.cfraser.connekt.api.SendChannel
import io.github.cfraser.connekt.api.SendChannel.Companion.asSendChannel
import io.github.cfraser.connekt.api.Transport
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.channels.Channel

/**
 * [TestTransport] is a [Transport] implementation that uses [Channel] to send and receive messages.
 *
 * [TestTransport] enables *local* testing of the functionality surrounding a [Transport].
 */
class TestTransport : Transport {

  private val channels: MutableMap<String, Channel<ByteArray>> = ConcurrentHashMap()
  private val messagesReceived = AtomicLong()
  private val messagesSent = AtomicLong()

  override fun receiveFrom(queue: String): ReceiveChannel<ByteArray> {
    return channel(queue).asReceiveChannel()
  }

  override fun sendTo(queue: String): SendChannel<ByteArray> {
    return channel(queue).asSendChannel()
  }

  override fun metrics() =
      Metrics(
          messagesReceived = messagesReceived.get(),
          messagesSent = messagesSent.get(),
          receiveErrors = 0,
          sendErrors = 0)

  override fun close() {}

  private fun channel(queue: String) =
      channels.computeIfAbsent(queue) {
        val channel = Channel<ByteArray>()
        object : Channel<ByteArray> by channel {
          override suspend fun receive() =
              channel.receive().also { messagesReceived.incrementAndGet() }
          override suspend fun send(element: ByteArray) =
              channel.send(element).also { messagesSent.incrementAndGet() }
        }
      }
}
