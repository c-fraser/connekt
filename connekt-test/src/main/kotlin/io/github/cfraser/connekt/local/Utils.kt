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

import io.github.cfraser.connekt.api.Deserializer
import io.github.cfraser.connekt.api.ReceiveChannel
import io.github.cfraser.connekt.api.Serializer
import io.github.cfraser.connekt.api.Transport
import java.time.Duration
import java.util.UUID
import java.util.function.Supplier
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * *Test* the [Transport].
 *
 * Verify the sending and receiving of random [String] data through queues.
 */
fun Transport.test() {
  test(
      { UUID.randomUUID().toString() },
      { message -> message.toByteArray() },
      { byteArray -> String(byteArray) })
}

/**
 * *Test* the [Transport].
 *
 * Verify the sending and receiving of messages through queues.
 *
 * @param T the type to send and receive
 * @param messageSupplier the [Supplier] for test message data
 * @param serializer the [Serializer] for [T]
 * @param deserializer the [Deserializer] for [T]
 */
fun <T> Transport.test(
    messageSupplier: Supplier<T>,
    serializer: Serializer<T>,
    deserializer: Deserializer<T>
) {
  use { transport ->
    val numberOfMessages = 1_000
    val sendMessages = (1..numberOfMessages).map { messageSupplier.get() }
    val numberOfQueues = 5
    val receivedMessages = arrayOfNulls<MutableList<T>>(numberOfQueues)

    runBlocking {
      val jobs = mutableListOf<Job>()

      repeat(numberOfQueues) { i ->
        receivedMessages[i] = mutableListOf()

        val queue = "$i"
        val sendChannel = transport.sendTo(queue, serializer)
        val receiveChannel: ReceiveChannel<T> = transport.receiveFrom(queue, deserializer)

        jobs +=
            launch {
              receivedMessages[i]!!.run {
                for (message in receiveChannel) {
                  this += message
                  logger.debug { "Received message $message from queue $queue" }
                  if (size == numberOfMessages) cancel()
                }
              }
            }

        delay(Duration.ofSeconds(1).toMillis())

        jobs +=
            launch {
              sendMessages.forEach { message ->
                sendChannel.send(message)
                logger.debug { "Sent message $message to queue $queue" }
              }
            }
      }

      withTimeout(Duration.ofSeconds(10).toMillis()) { jobs.joinAll() }
    }

    receivedMessages.forEach { messages -> assertTrue { messages!!.containsAll(sendMessages) } }

    with(transport.metrics()) {
      assertEquals(0L, receiveErrors)
      assertEquals(0L, sendErrors)
    }
  }
}
