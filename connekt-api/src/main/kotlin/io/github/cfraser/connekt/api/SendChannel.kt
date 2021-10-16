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
import kotlinx.coroutines.runBlocking

/**
 * [SendChannel] is a [KotlinxCoroutinesSendChannel] plus [sendSync] and [sendAsync] which enables
 * idiomatic usage from *Java* code.
 *
 * @see KotlinxCoroutinesSendChannel
 */
interface SendChannel<in T> : KotlinxCoroutinesSendChannel<T> {

  /** Synchronously [send] the [message] to this channel. */
  fun sendSync(message: T) {
    runBlocking(Dispatchers.IO) { send(message) }
  }

  /**
   * Asynchronously [send] the [message] to this channel.
   *
   * @return the [CompletableFuture] containing the result of the asynchronous operation
   */
  fun sendAsync(message: T): CompletableFuture<Unit> {
    return GlobalScope.future(Dispatchers.IO) { send(message) }
  }
}
