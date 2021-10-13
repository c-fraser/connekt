@file:OptIn(ExperimentalTransport::class)
@file:Suppress("PackageDirectoryMismatch")
// This file was automatically generated from README.md by Knit tool. Do not edit.
package io.github.cfraser.connekt.example.knit.transportExample01

import io.github.cfraser.connekt.api.ExperimentalTransport
import io.github.cfraser.connekt.rsocket.RSocketTransport
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

fun main() {
  RSocketTransport.Builder()
      .queueDestinationResolver { setOf(InetSocketAddress(8787)) }
      .build()
      .use { transport ->
        val receiveChannel = transport.receiveFrom("print-message")
        val sendChannel = transport.sendTo("print-message")
        runBlocking {
          val message = async { String(receiveChannel.receive()) }
          sendChannel.send("Hello, world!".toByteArray())
          println(message.await())
        }
      }
}
