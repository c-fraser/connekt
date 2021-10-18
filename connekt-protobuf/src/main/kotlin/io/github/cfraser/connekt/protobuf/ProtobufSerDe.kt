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

import com.google.protobuf.Message
import io.github.cfraser.connekt.api.Deserializer
import io.github.cfraser.connekt.api.Serializer
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 * The [ProtobufSerDe] object provides functions to create [Serializer] and [Deserializer] instances
 * that use [Protocol Buffers](https://developers.google.com/protocol-buffers/) for serialization
 * and deserialization.
 */
object ProtobufSerDe {

  /**
   * Initialize a [Serializer] which uses [Message.toByteArray] to serialize instances of [T].
   *
   * @param T the type to serialize
   * @return the [Serializer]
   */
  @JvmStatic
  fun <T : Message> serializer(): Serializer<T> {
    return Serializer { message -> message.toByteArray() }
  }

  /**
   * Initialize a [Deserializer] which uses the generated `public static T parseFrom(byte[] data)`
   * method within [T] to construct instances of [T].
   *
   * @param T the [Message] type to deserialize
   * @param clazz the [Class] for [T]
   * @return the [Deserializer]
   */
  @JvmStatic
  fun <T : Message> deserializer(
      clazz: Class<T>,
  ): Deserializer<T> {
    return Deserializer @Suppress("UNCHECKED_CAST")
    { byteArray ->
      val methodLookup = MethodHandles.publicLookup()
      val methodType = MethodType.methodType(clazz, ByteArray::class.java)
      val methodHandle = methodLookup.findStatic(clazz, "parseFrom", methodType)
      methodHandle.invoke(byteArray) as T
    }
  }

  /**
   * Initialize a [Deserializer] which uses the generated `public static T parseFrom(byte[] data)`
   * method within [T] to construct instances of [T].
   *
   * @param T the [Message] type to deserialize
   * @return the [Deserializer]
   */
  inline fun <reified T : Message> deserializer(): Deserializer<T> {
    return deserializer(T::class.java)
  }
}
