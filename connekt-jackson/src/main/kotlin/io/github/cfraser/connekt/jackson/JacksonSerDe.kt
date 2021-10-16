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

/** The [ObjectMapper] used for serialization and deserialization. */
val OBJECT_MAPPER: ObjectMapper by lazy { jacksonObjectMapper() }

/**
 * Initialize a [Serializer] which uses [OBJECT_MAPPER] to serialize instances of [T].
 *
 * @param T the type to serialize
 * @return the [Serializer]
 */
inline fun <reified T : Any> jacksonSerializer(): Serializer<T> = Serializer { message ->
  OBJECT_MAPPER.writeValueAsBytes(message)
}

/**
 * Initialize a [Deserializer] which uses [OBJECT_MAPPER] to construct instances of [T].
 *
 * @param T the type to deserialize
 * @return the [Deserializer]
 */
inline fun <reified T : Any> jacksonDeserializer(): Deserializer<T> = Deserializer { byteArray ->
  OBJECT_MAPPER.readValue(byteArray, T::class.java)
}
