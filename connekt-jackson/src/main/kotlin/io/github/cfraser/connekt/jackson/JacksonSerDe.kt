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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.cfraser.connekt.api.Deserializer
import io.github.cfraser.connekt.api.Serializer

/**
 * The [JacksonSerDe] object provides functions to create [Serializer] and [Deserializer] instances
 * that use [Jackson](https://github.com/FasterXML/jackson) for serialization and deserialization.
 */
object JacksonSerDe {

  /** A default [ObjectMapper] to use for serialization and deserialization. */
  @JvmStatic val OBJECT_MAPPER: ObjectMapper by lazy { jacksonObjectMapper() }

  /**
   * Initialize a [Serializer] which uses [objectMapper] to serialize instances of [T].
   *
   * @param T the type to serialize
   * @param objectMapper the [ObjectMapper] to use to serialize instances of [T]
   * @return the [Serializer]
   */
  @JvmOverloads
  @JvmStatic
  fun <T : Any> serializer(objectMapper: ObjectMapper = OBJECT_MAPPER): Serializer<T> {
    return Serializer { message -> objectMapper.writeValueAsBytes(message) }
  }

  /**
   * Initialize a [Deserializer] which uses [objectMapper] to construct instances of [T].
   *
   * @param T the type to deserialize
   * @param clazz the [Class] for [T]
   * @param objectMapper the [ObjectMapper] to use to deserialize instances of [T]
   * @return the [Deserializer]
   */
  @JvmOverloads
  @JvmStatic
  fun <T : Any> deserializer(
      clazz: Class<T>,
      objectMapper: ObjectMapper = OBJECT_MAPPER
  ): Deserializer<T> {
    return Deserializer { byteArray -> objectMapper.readValue(byteArray, clazz) }
  }

  /**
   * Initialize a [Deserializer] which uses [objectMapper] to construct instances of [T].
   *
   * @param T the type to deserialize
   * @param objectMapper the [ObjectMapper] to use to deserialize instances of [T]
   * @return the [Deserializer]
   */
  inline fun <reified T : Any> deserializer(
      objectMapper: ObjectMapper = OBJECT_MAPPER
  ): Deserializer<T> {
    return deserializer(T::class.java, objectMapper)
  }
}
