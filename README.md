# connekt

[![Build](https://github.com/c-fraser/connekt/workflows/Build/badge.svg)](https://github.com/c-fraser/connekt/actions)
[![Release](https://img.shields.io/github/v/release/c-fraser/connekt?logo=github&sort=semver)](https://github.com/c-fraser/connekt/releases)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.c-fraser/connekt-api.svg)](https://search.maven.org/artifact/io.github.c-fraser/connekt-api)
[![Javadoc](https://javadoc.io/badge2/io.github.c-fraser/connekt-api/javadoc.svg)](https://javadoc.io/doc/io.github.c-fraser/connekt-api)
[![Apache License 2.0](https://img.shields.io/badge/License-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

Connect Kotlin/Java applications using
distributed [channels](https://kotlinlang.org/docs/channels.html), augmented with a message queuing
technology to enable horizontal scalability and resiliency.

## Design

The `connekt` API implementations allow the user to leverage distributed messaging transparently
through Kotlin coroutine channels. The project supports various transports which are easily
interchangeable, which empowers the user to utilize the messaging technology that is most suitable
for the problem domain.

## Usage

The `connekt` libraries are accessible
via [Maven Central](https://search.maven.org/search?q=g:io.github.c-fraser%20AND%20a:connekt-*).

### RSocket

<!--- TEST_NAME RSocketTransportExampleTest --> 

Initialize a `RSocketTransport` then send and receive a message.

<!--- PREFIX
@file:Suppress("PackageDirectoryMismatch")
-->

<!--- INCLUDE
import io.github.cfraser.connekt.rsocket.RSocketTransport
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
-->

```kotlin 
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
```                         

<!--- KNIT rsocket-transport-example-01.kt --> 

<!--- TEST
Hello, world!
-->

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

`connekt` was inspired by [vice](https://github.com/matryer/vice). 