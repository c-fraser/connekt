package io.github.cfraser.graphit

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class QueueTests :
    StringSpec({
      "verify a FIFO queue" { FIFOQueue<Int>().items shouldContainExactly (0 until ITEMS).toList() }
      "verify a LIFO queue" {
        LIFOQueue<Int>().items shouldContainExactly ((ITEMS - 1) downTo 0).toList()
      }
      "verify a priority queue" {
        val queue = PriorityQueue<Char>()
        val letters = ('a'..'z').toList()
        letters.mapIndexed { i, c -> c weighs i }.shuffled().forEach(queue::offer)
        queue.items().map { it.value } shouldContainExactly letters
      }
      "verify resetting priority of an existing value in priority queue" {
        val queue = PriorityQueue<Int>()
        repeat(ITEMS) { queue.offer(it weighs it) }
        queue.poll()?.value shouldBe 0
        val item = ITEMS - 1
        queue[item] = 0f
        queue.poll()?.value shouldBe item
        queue.items().map { it.value } shouldContainExactly (1 until item).toList()
      }
    }) {

  private companion object {

    const val ITEMS = 10

    val Queue<Int>.items: List<Int>
      get() {
        repeat(ITEMS) { offer(it) }
        return items()
      }

    fun <T : Any> Queue<T>.items(): List<T> = buildList {
      while (true) {
        this += poll() ?: break
      }
    }
  }
}
