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
package io.github.cfraser.connekt.api

/**
 * A [Serializer] is a function that converts an instance of [T] to a [ByteArray].
 *
 * @param T the type to convert to bytes
 */
fun interface Serializer<in T> {

  /**
   * Convert the [message] to a [ByteArray].
   *
   * @param message the instance of [T] to serialize
   * @return the serialized message
   */
  fun serialize(message: T): ByteArray
}

/**
 * A [Deserializer] is a function that converts a [ByteArray] to an instance of [T].
 *
 * @param T the type to convert the bytes to
 */
fun interface Deserializer<out T> {

  /**
   * Convert the [byteArray] to an instance of [T].
   *
   * @param byteArray the [ByteArray] to deserialize
   * @return the deserialized message
   */
  fun deserialize(byteArray: ByteArray): T
}
