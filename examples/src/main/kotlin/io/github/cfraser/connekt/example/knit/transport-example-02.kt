@file:Suppress("PackageDirectoryMismatch")
// This file was automatically generated from README.md by Knit tool. Do not edit.
package io.github.cfraser.connekt.example.knit.transportExample02

import io.github.cfraser.connekt.api.Transport

fun example02(transport: Transport) {

val receiveChannel = transport.receiveFrom("print-message") { byteArray -> String(byteArray) }
val sendChannel = transport.sendTo("print-message") { message: String -> message.toByteArray() }
sendChannel.sendSync("Hello, world!")
println(receiveChannel.receiveSync())
}
