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

import java.io.Closeable

/**
 * [Transport] provides message sending and receiving capabilities using a message queuing
 * technology.
 *
 * Messages are asynchronously sent to a *queue* using the [SendChannel] returned by [sendTo], and
 * messages are asynchronously received from a *queue* using the [ReceiveChannel] returned by
 * [receiveFrom].
 */
interface Transport : Closeable {

  /**
   * Initialize a [ReceiveChannel] to [ReceiveChannel.receive] messages from the [queue].
   *
   * @param queue the *queue* to receive messages from
   * @return the [ReceiveChannel]
   */
  fun receiveFrom(queue: String): ReceiveChannel<ByteArray>

  /**
   * Initialize a [SendChannel] to [SendChannel.send] messages to the [queue].
   *
   * @param queue the *queue* to send messages to
   * @return the [SendChannel]
   */
  fun sendTo(queue: String): SendChannel<ByteArray>

  /**
   * Get the [Metrics] for the [Transport].
   *
   * @return the [Metrics]
   */
  fun metrics(): Metrics
}
