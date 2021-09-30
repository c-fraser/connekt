@file:Suppress("PackageDirectoryMismatch")
// This file was automatically generated from README.md by Knit tool. Do not edit.
package io.github.cfraser.connekt.example.rsocketTransportExample01

import io.github.cfraser.connekt.rsocket.RSocketTransport
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

fun main() {
  RSocketTransport.new({ setOf(InetSocketAddress(8787)) }).use { transport ->
    runBlocking {
      val receiveChannel = transport.receiveFrom("example")
      val message = async { String(receiveChannel.receive()) }
      val sendChannel = transport.sendTo("example")
      sendChannel.send("Hello, world!".toByteArray())
      println(message.await())
    }
  }
}
