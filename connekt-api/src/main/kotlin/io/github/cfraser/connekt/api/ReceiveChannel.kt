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

import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future

/**
 * [ReceiveChannel] is a [kotlinx.coroutines.channels.ReceiveChannel] plus [receiveAsync] which
 * enables idiomatic usage from *Java* code.
 *
 * @see kotlinx.coroutines.channels.ReceiveChannel
 */
interface ReceiveChannel<out E> : kotlinx.coroutines.channels.ReceiveChannel<E> {

  /**
   * Asynchronously [receive] the next *element* from this channel.
   *
   * @return the [CompletableFuture] containing the result of the asynchronous operation
   */
  fun receiveAsync(): CompletableFuture<out E> {
    return GlobalScope.future(Dispatchers.IO) { receive() }
  }

  companion object {

    /**
     * Convert the [kotlinx.coroutines.channels.ReceiveChannel] to a [ReceiveChannel].
     *
     * @return the [ReceiveChannel]
     */
    fun <E> kotlinx.coroutines.channels.ReceiveChannel<E>.toReceiveChannel(): ReceiveChannel<E> {
      return object : ReceiveChannel<E>, kotlinx.coroutines.channels.ReceiveChannel<E> by this {}
    }
  }
}
