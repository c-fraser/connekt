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
import kotlinx.coroutines.channels.SendChannel as KotlinxCoroutinesSendChannel
import kotlinx.coroutines.future.future

/**
 * [SendChannel] is a [KotlinxCoroutinesSendChannel] plus [sendAsync] which enables idiomatic
 * non-blocking usage from *Java* code.
 *
 * @see KotlinxCoroutinesSendChannel
 */
interface SendChannel<in E> : KotlinxCoroutinesSendChannel<E> {

  /**
   * Asynchronously [send] the [element] to this channel.
   *
   * @return the [CompletableFuture] containing the result of the asynchronous operation
   */
  fun sendAsync(element: E): CompletableFuture<Unit> {
    return GlobalScope.future(Dispatchers.IO) { send(element) }
  }

  companion object {

    /**
     * Use the [KotlinxCoroutinesSendChannel] as a [SendChannel].
     *
     * @return the [SendChannel]
     */
    @InternalConnektApi
    fun <E> KotlinxCoroutinesSendChannel<E>.asSendChannel(): SendChannel<E> {
      return object : KotlinxCoroutinesSendChannel<E> by this, SendChannel<E> {}
    }
  }
}
