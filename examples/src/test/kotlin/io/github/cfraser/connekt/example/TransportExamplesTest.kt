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
package io.github.cfraser.connekt.example

import kotlin.test.Test
import kotlinx.knit.test.captureOutput
import kotlinx.knit.test.verifyOutputLines
import org.junit.jupiter.api.Tag

@Tag("integration")
class TransportExamplesTest {

  @Test
  fun testRedisTransportExample() {
    captureOutput("JavaRedisTransportExample") { TransportExamples.redisExample() }
        .verifyOutputLines("Hello, world!")
  }

  @Test
  fun testRsocketTransportExample() {
    captureOutput("JavaRSocketTransportExample") { TransportExamples.rsocketExample() }
        .verifyOutputLines("Hello, world!")
  }
}
