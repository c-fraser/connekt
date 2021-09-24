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
import kotlinx.coroutines.channels.SendChannel

/**
 * [Transport] provides message sending and receiving capabilities using a messaging technology.
 *
 * Each message is sent to a *topic*, subsequently the message is only received by receiving from
 * the same *topic*.
 *
 * > A *topic* is a case-sensitive [String] that cannot contain whitespace.
 */
interface Transport : Closeable {

  /**
   * Initialize a [ReceiveChannel] to [ReceiveChannel.receive] messages sent to the [topic].
   *
   * @param topic the *topic* to receive messages from
   * @return the [ReceiveChannel]
   */
  fun receive(topic: String): ReceiveChannel<ByteArray>

  /**
   * Initialize a [SendChannel] to [SendChannel.send] messages to the topic.
   *
   * @param topic the *topic* to send messages to
   * @return the [SendChannel]
   */
  fun send(topic: String): SendChannel<ByteArray>

  /**
   * Get the [Metrics] for the [Transport].
   *
   * @return the [Metrics]
   */
  fun metrics(): Metrics

  /** [Metrics] contains data collected for a [Transport]. */
  interface Metrics {

    /** The total number of messages received by the [Transport]. */
    val messagesReceived: Long

    /** The total number of messages sent by the [Transport]. */
    val messagesSent: Long

    /** The total number of receive errors for the [Transport]. */
    val receiveErrors: Long

    /** The total number of send errors for the [Transport]. */
    val sendErrors: Long
  }
}
