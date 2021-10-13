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

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.michaelbull.retry.policy.binaryExponentialBackoff
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future

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

/**
 * [CachingQueueDestinationResolver] is a [QueueDestinationResolver] that caches the *queue*
 * destination IP socket address(es) resolved by the [delegate].
 *
 * @property delegate the [QueueDestinationResolver] that resolves the destination IP socket
 * address(es) for the *queue*
 */
class CachingQueueDestinationResolver(private val delegate: QueueDestinationResolver) :
    QueueDestinationResolver {

  /**
   * [AsyncLoadingCache] containing the resolved the IP socket address(es) for each *queue*.
   *
   * Entries are automatically refreshed every minute.
   */
  private val asyncLoadingCache: AsyncLoadingCache<String, Set<InetSocketAddress>> by lazy {
    Caffeine.newBuilder().maximumSize(64).refreshAfterWrite(Duration.ofMinutes(1)).buildAsync {
        queue,
        executor ->
      GlobalScope.future(executor.asCoroutineDispatcher()) {
        runCatching {
          retry(limitAttempts(5) + binaryExponentialBackoff(100L..10_000L)) {
            delegate.resolve(queue)
          }
        }
            .getOrNull()
            ?.takeUnless { it.isEmpty() }
      }
    }
  }

  override suspend fun resolve(queue: String): Set<InetSocketAddress> {
    return runCatching { asyncLoadingCache[queue].await() }.getOrDefault(emptySet())
  }
}
