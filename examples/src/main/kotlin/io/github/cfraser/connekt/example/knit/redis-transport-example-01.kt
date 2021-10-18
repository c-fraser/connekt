@file:Suppress("PackageDirectoryMismatch")
// This file was automatically generated from README.md by Knit tool. Do not edit.
package io.github.cfraser.connekt.example.knit.redisTransportExample01

import io.github.cfraser.connekt.example.knit.transportExample01.example01
import io.github.cfraser.connekt.example.knit.transportExample02.example02
import io.github.cfraser.connekt.redis.RedisTransport
import io.lettuce.core.RedisURI

fun main() {

RedisTransport.Builder()
      .redisURI(RedisURI.create("localhost", RedisURI.DEFAULT_REDIS_PORT))
      .build()
    .use { transport -> 
        example01(transport)
        example02(transport)
    }
}
