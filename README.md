# connekt

[![Build](https://github.com/c-fraser/connekt/workflows/build/badge.svg)](https://github.com/c-fraser/connekt/actions)
[![Release](https://img.shields.io/github/v/release/c-fraser/connekt?logo=github&sort=semver)](https://github.com/c-fraser/connekt/releases)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.c-fraser/connekt-api.svg)](https://search.maven.org/artifact/io.github.c-fraser/connekt-api)
[![Javadoc](https://javadoc.io/badge2/io.github.c-fraser/connekt-api/javadoc.svg)](https://javadoc.io/doc/io.github.c-fraser/connekt-api)
[![Apache License 2.0](https://img.shields.io/badge/License-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

Connect Kotlin/Java applications using
[channels](https://kotlinlang.org/docs/channels.html) augmented with a messaging technology to
enable horizontal scalability.

## Contents

<!--- TOC -->

* [Overview](#overview)
* [Usage](#usage)
  * [Transports](#transports)
    * [NATS](#nats)
    * [Redis](#redis)
    * [RSocket](#rsocket)
    * [Local](#local)
  * [SerDes](#serdes)
    * [Jackson](#jackson)
    * [Protobuf](#protobuf)
* [License](#license)
* [Acknowledgements](#acknowledgements)

<!--- END -->

## Overview

The implementations of
the [connekt-api](https://javadoc.io/doc/io.github.c-fraser/connekt-api/latest/index.html) allow the
user to leverage distributed messaging transparently through Kotlin coroutine channels. The project
supports various transports which are easily interchangeable, which empowers the user to utilize the
messaging technology that is most suitable for the problem domain.

## Usage

The `connekt` libraries are accessible
via [Maven Central](https://search.maven.org/search?q=g:io.github.c-fraser%20AND%20a:connekt-*).

> Java 11+ is required to use the `connekt` libraries.

[Use](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/use.html)
a [Transport](https://javadoc.io/doc/io.github.c-fraser/connekt-api/latest/io/github/cfraser/connekt/api/Transport.html)
to send and receive a message.

<!--- PREFIX
@file:Suppress("PackageDirectoryMismatch")
-->

<!--- INCLUDE
import io.github.cfraser.connekt.api.Transport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun example01(transport: Transport) {
----- SUFFIX
}
-->

```kotlin 
val receiveChannel = transport.receiveFrom("print-message")
val sendChannel = transport.sendTo("print-message")
runBlocking(Dispatchers.IO) {
  val message = async { String(receiveChannel.receive()) }
  delay(1_000)
  sendChannel.send("Hello, world!".toByteArray())
  println(message.await())
}
```                         

<!--- KNIT transport-example-01.kt -->

A [Serializer](https://javadoc.io/doc/io.github.c-fraser/connekt-api/latest/io/github/cfraser/connekt/api/Serializer.html)
or [Deserializer](https://javadoc.io/doc/io.github.c-fraser/connekt-api/latest/io/github/cfraser/connekt/api/Deserializer.html)
may be provided to enable the `Transport` to automatically handle the (de)serialization of messages.

<!--- PREFIX
@file:Suppress("PackageDirectoryMismatch")
-->

<!--- INCLUDE
import io.github.cfraser.connekt.api.Transport

fun example02(transport: Transport) {
----- SUFFIX
}
-->

```kotlin 
val receiveChannel = transport.receiveFrom("print-message") { byteArray -> String(byteArray) }
val sendChannel = transport.sendTo("print-message") { message: String -> message.toByteArray() }
sendChannel.sendSync("Hello, world!")
println(receiveChannel.receiveSync())
```                         

<!--- KNIT transport-example-02.kt --> 

> Refer
> to [these examples](https://github.com/c-fraser/connekt/tree/main/examples/src/main/java/io/github/cfraser/connekt/example/TransportExamples.java)
> to see how to use `Transport` in **Java**.

### Transports

#### NATS

<!--- TEST_NAME NatsTransportExampleTest --> 

Use [connekt-nats](https://search.maven.org/artifact/io.github.c-fraser/connekt-nats)
to [build](https://javadoc.io/doc/io.github.c-fraser/connekt-redis/latest/io/github/cfraser/connekt/redis/RedisTransport.Builder.html)
a [NatsTransport](https://javadoc.io/doc/io.github.c-fraser/connekt-nats/latest/io/github/cfraser/connekt/nats/NatsTransport.html)
that sends and receives messages using [NATS](https://nats.io/).

<!--- PREFIX
@file:Suppress("PackageDirectoryMismatch")
-->

<!--- INCLUDE
import io.github.cfraser.connekt.example.knit.transportExample01.example01
import io.github.cfraser.connekt.example.knit.transportExample02.example02
import io.github.cfraser.connekt.nats.NatsTransport

fun main() {
----- SUFFIX
    .use { transport -> 
        example01(transport)
        example02(transport)
    }
}
-->

```kotlin 
NatsTransport.Builder().serverURL("nats://localhost:4222").build()
```                         

<!--- KNIT nats-transport-example-01.kt --> 

<!--- TEST
Hello, world!
Hello, world!
-->

#### Redis

<!--- TEST_NAME RedisTransportExampleTest --> 

Use [connekt-redis](https://search.maven.org/artifact/io.github.c-fraser/connekt-redis)
to [build](https://javadoc.io/doc/io.github.c-fraser/connekt-redis/latest/io/github/cfraser/connekt/redis/RedisTransport.Builder.html)
a [RedisTransport](https://javadoc.io/doc/io.github.c-fraser/connekt-redis/latest/io/github/cfraser/connekt/redis/RedisTransport.html)
that sends and receives messages using [Redis Pub/Sub](https://redis.io/topics/pubsub).

<!--- PREFIX
@file:Suppress("PackageDirectoryMismatch")
-->

<!--- INCLUDE
import io.github.cfraser.connekt.example.knit.transportExample01.example01
import io.github.cfraser.connekt.example.knit.transportExample02.example02
import io.github.cfraser.connekt.redis.RedisTransport
import io.lettuce.core.RedisURI

fun main() {
----- SUFFIX
    .use { transport -> 
        example01(transport)
        example02(transport)
    }
}
-->

```kotlin 
RedisTransport.Builder().redisURI(RedisURI.create("localhost", RedisURI.DEFAULT_REDIS_PORT)).build()
```                         

<!--- KNIT redis-transport-example-01.kt --> 

<!--- TEST
Hello, world!
Hello, world!
-->

#### RSocket

<!--- TEST_NAME RSocketTransportExampleTest --> 

Use [connekt-rsocket](https://search.maven.org/artifact/io.github.c-fraser/connekt-rsocket)
to [build](https://javadoc.io/doc/io.github.c-fraser/connekt-rsocket/latest/io/github/cfraser/connekt/rsocket/RSocketTransport.Builder.html)
a [RSocketTransport](https://javadoc.io/doc/io.github.c-fraser/connekt-rsocket/latest/io/github/cfraser/connekt/rsocket/RSocketTransport.html)
that sends and receives messages using [RSocket](https://rsocket.io/).

<!--- PREFIX
@file:OptIn(ExperimentalTransport::class)
@file:Suppress("PackageDirectoryMismatch")
-->

<!--- INCLUDE
import io.github.cfraser.connekt.api.ExperimentalTransport
import io.github.cfraser.connekt.example.knit.transportExample01.example01
import io.github.cfraser.connekt.example.knit.transportExample02.example02
import io.github.cfraser.connekt.rsocket.RSocketTransport
import java.net.InetSocketAddress

fun main() {
----- SUFFIX
    .use { transport -> 
        example01(transport)
        example02(transport)
    }
}
-->

```kotlin 
RSocketTransport.Builder().queueDestinationResolver { setOf(InetSocketAddress(8787)) }.build()
```                         

<!--- KNIT rsocket-transport-example-01.kt --> 

<!--- TEST
Hello, world!
Hello, world!
-->

#### Local

Use [connekt-local](https://search.maven.org/artifact/io.github.c-fraser/connekt-local)
to construct
a [LocalTransport](https://javadoc.io/doc/io.github.c-fraser/connekt-local/latest/io/github/cfraser/connekt/local/LocalTransport.html)
that sends and receives messages using *in-memory* channels.

### SerDes

#### Jackson

Use [connekt-jackson](https://search.maven.org/artifact/io.github.c-fraser/connekt-jackson)
to [(de)serialize](https://javadoc.io/doc/io.github.c-fraser/connekt-jackson/latest/io/github/cfraser/connekt/jackson/JacksonSerDe.html)
messages using [Jackson](https://github.com/FasterXML/jackson).

#### Protobuf

Use [connekt-protobuf](https://search.maven.org/artifact/io.github.c-fraser/connekt-protobuf)
to [(de)serialize](https://javadoc.io/doc/io.github.c-fraser/connekt-protobuf/latest/io/github/cfraser/connekt/protobuf/ProtobufSerDe.html)
messages using [Protocol buffers](https://developers.google.com/protocol-buffers).

## License

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

## Acknowledgements

Kudos to the [vice](https://github.com/matryer/vice) project which was the inspiration for
`connekt`.
