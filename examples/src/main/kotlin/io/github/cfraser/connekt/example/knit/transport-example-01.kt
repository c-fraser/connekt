@file:Suppress("PackageDirectoryMismatch")
// This file was automatically generated from README.md by Knit tool. Do not edit.
package io.github.cfraser.connekt.example.knit.transportExample01

import io.github.cfraser.connekt.api.Transport
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun Transport.example01() {

use { transport ->
    val receiveChannel = transport.receiveFrom("print-message")
    val sendChannel = transport.sendTo("print-message")
    runBlocking {
      val message = async { String(receiveChannel.receive()) }
      delay(1_000)
      sendChannel.send("Hello, world!".toByteArray())
      println(message.await())
    }
  }
}
