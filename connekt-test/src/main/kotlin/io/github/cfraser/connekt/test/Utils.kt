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
package io.github.cfraser.connekt.test

import io.github.cfraser.connekt.api.Transport
import java.time.Duration
import java.util.UUID
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
 * Verify the sending and receiving or messages through queues.
 */
fun Transport.test() {
  use { transport ->
    val numberOfMessages = 1_000
    val sendMessages = (1..numberOfMessages).map { UUID.randomUUID().toString() }
    val numberOfQueues = 5
    val receivedMessages = arrayOfNulls<MutableList<String>>(numberOfQueues)

    runBlocking {
      val jobs = mutableListOf<Job>()

      repeat(numberOfQueues) { i ->
        receivedMessages[i] = mutableListOf()

        val queue = "queue-$i"
        val sendChannel = transport.sendTo(queue)
        val receiveChannel = transport.receiveFrom(queue)

        jobs +=
            launch {
              receivedMessages[i]!!.run {
                for (byteArray in receiveChannel) {
                  this +=
                      String(byteArray).also { message ->
                        logger.debug { "Received message $message to queue $queue" }
                      }
                  if (size == numberOfMessages) cancel()
                }
              }
            }

        delay(Duration.ofSeconds(1).toMillis())

        jobs +=
            launch {
              sendMessages.map { message -> message.toByteArray() }.forEach { message ->
                sendChannel.send(message)
                logger.debug { "Sent message ${String(message)} to queue $queue" }
              }
            }
      }

      withTimeout(Duration.ofSeconds(10).toMillis()) { jobs.joinAll() }
    }

    receivedMessages.forEach { assertTrue { it!!.containsAll(sendMessages) } }

    with(transport.metrics()) {
      assertEquals(0L, receiveErrors)
      assertEquals(0L, sendErrors)
    }
  }
}
