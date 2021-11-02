@file:OptIn(ExperimentalTransport::class)
@file:Suppress("PackageDirectoryMismatch")
// This file was automatically generated from README.md by Knit tool. Do not edit.
package io.github.cfraser.connekt.example.knit.rsocketTransportExample01

import io.github.cfraser.connekt.api.ExperimentalTransport
import io.github.cfraser.connekt.example.knit.transportExample01.example01
import io.github.cfraser.connekt.example.knit.transportExample02.example02
import io.github.cfraser.connekt.rsocket.RSocketTransport
import java.net.InetSocketAddress

fun main() {

RSocketTransport.Builder().queueDestinationResolver { setOf(InetSocketAddress(8787)) }.build()
    .use { transport -> 
        example01(transport)
        example02(transport)
    }
}
