@file:Suppress("PackageDirectoryMismatch")
// This file was automatically generated from README.md by Knit tool. Do not edit.
package io.github.cfraser.connekt.example.knit.natsTransportExample01

import io.github.cfraser.connekt.example.knit.transportExample01.example01
import io.github.cfraser.connekt.example.knit.transportExample02.example02
import io.github.cfraser.connekt.nats.NatsTransport

fun main() {

NatsTransport.Builder().serverURL("nats://localhost:4222").build()
    .use { transport -> 
        example01(transport)
        example02(transport)
    }
}
