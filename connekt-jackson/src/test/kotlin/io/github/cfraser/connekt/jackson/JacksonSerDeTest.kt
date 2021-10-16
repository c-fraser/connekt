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
package io.github.cfraser.connekt.jackson

import io.github.cfraser.connekt.test.LocalTransport
import io.github.cfraser.connekt.test.test
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class JacksonSerDeTest {

  @Test
  fun testSerDe() {
    val serializer = jacksonSerializer<TestMessage>()
    val deserializer = jacksonDeserializer<TestMessage>()
    val testMessage = testMessage()
    val serialized = serializer.serialize(testMessage)
    val deserialized = deserializer.deserialize(serialized)
    assertEquals(testMessage, deserialized)
  }

  @Test
  fun testTransportSerDe() {
    LocalTransport().test(::testMessage, jacksonSerializer(), jacksonDeserializer())
  }

  data class TestMessage(val a: String, var b: Int, val c: Boolean, var d: Double, val e: Long)

  companion object {

    fun testMessage() =
        TestMessage(
            UUID.randomUUID().toString(),
            Random.nextInt(),
            Random.nextBoolean(),
            Random.nextDouble(),
            Random.nextLong())
  }
}
