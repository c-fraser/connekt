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
 * [Metrics] contains data collected for a [Transport].
 *
 * @property messagesReceived the total number of messages received by the [Transport]
 * @property messagesSent the total number of messages sent by the [Transport]
 * @property receiveErrors the total number of receive errors for the [Transport]
 * @property sendErrors the total number of send errors for the [Transport]
 */
data class Metrics(
    val messagesReceived: Long,
    val messagesSent: Long,
    val receiveErrors: Long,
    val sendErrors: Long
) {

  constructor() : this(0, 0, 0, 0)
}
