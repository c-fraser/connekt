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
package io.github.cfraser.connekt.protobuf

import io.github.cfraser.connekt.local.LocalTransport
import io.github.cfraser.connekt.local.test
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtobufSerDeTest {

  @Test
  fun testSerDe() {
    val serializer = ProtobufSerDe.serializer<TestMessage>()
    val deserializer = ProtobufSerDe.deserializer<TestMessage>()
    val testMessage = testMessage()
    val serialized = serializer.serialize(testMessage)
    val deserialized = deserializer.deserialize(serialized)
    assertEquals(testMessage, deserialized)
  }

  @Test
  fun testTransportSerDe() {
    LocalTransport()
        .test<TestMessage>(::testMessage, ProtobufSerDe.serializer(), ProtobufSerDe.deserializer())
  }

  companion object {

    fun testMessage(): TestMessage =
        TestMessage.newBuilder()
            .setA(UUID.randomUUID().toString())
            .setB(Random.nextInt())
            .setC(Random.nextBoolean())
            .setD(Random.nextDouble())
            .setE(Random.nextLong())
            .build()
  }
}
