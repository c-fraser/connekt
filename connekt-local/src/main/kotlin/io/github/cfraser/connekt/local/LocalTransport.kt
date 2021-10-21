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
package io.github.cfraser.connekt.local

import io.github.cfraser.connekt.api.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull

/**
 * [LocalTransport] is a [Transport] implementation that uses [MutableSharedFlow] to send and
 * receive messages.
 *
 * [LocalTransport] enables *local* testing of the functionality surrounding a [Transport].
 */
class LocalTransport :
    Transport by object : Transport.Base() {

      private val mutableSharedFlow = MutableSharedFlow<Pair<String, ByteArray>>()

      override fun CoroutineScope.receive(queue: String): Flow<ByteArray> {
        return mutableSharedFlow.mapNotNull { (sentTo, byteArray) ->
          byteArray.takeIf { sentTo == queue }
        }
      }

      override suspend fun send(queue: String, byteArray: ByteArray) {
        mutableSharedFlow.emit(queue to byteArray)
      }

      override fun close() {}
    }
