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
package io.github.cfraser.connekt.rsocket

import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.future.await

/**
 * The [QueueDestinationResolver] function type is responsible for determining the destination IP
 * socket address(es) for a *queue*.
 */
fun interface QueueDestinationResolver {

  /**
   * Return the resolved the IP socket address(es).
   *
   * @param queue the *queue* to get the IP socket address(es) for
   * @return the [InetSocketAddress] instances for the *queue*
   */
  suspend fun resolve(queue: String): Set<InetSocketAddress>
}

/**
 * [AsyncQueueDestinationResolver] is a [QueueDestinationResolver] that uses [CompletableFuture]
 * instead of `suspend` which enables a [QueueDestinationResolver] to be implemented idiomatically
 * in *Java* code.
 */
fun interface AsyncQueueDestinationResolver : QueueDestinationResolver {

  /**
   * Return the [CompletableFuture] with the resolved the IP socket address(es).
   *
   * @param queue the *queue* to get the IP socket address(es) for
   * @return the [CompletableFuture] of [InetSocketAddress] instances for the *queue*
   */
  fun routeAsync(queue: String): CompletableFuture<Set<InetSocketAddress>>

  override suspend fun resolve(queue: String): Set<InetSocketAddress> {
    return routeAsync(queue).await()
  }
}
