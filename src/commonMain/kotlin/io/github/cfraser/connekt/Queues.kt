/*
Copyright 2022 c-fraser

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
package io.github.cfraser.connekt

/** [Queue] is a minimal interface for the queuing of items. */
internal sealed interface Queue<T : Any> {

  /** Insert the [item] into the queue. */
  fun offer(item: T)

  /**
   * Retrieve and remove the next item in the queue.
   * > `null` is returned if the queue is empty.
   */
  fun poll(): T?
}

/** [FIFOQueue] is a *first in first out* [Queue] implementation. */
internal class FIFOQueue<T : Any> : Queue<T> {

  private val items = mutableListOf<T>()

  override fun offer(item: T) {
    items += item
  }

  override fun poll(): T? = items.removeFirstOrNull()
}

/** [LIFOQueue] is a *last in first out* [Queue] implementation. */
internal class LIFOQueue<T : Any> : Queue<T> {

  private val items = mutableListOf<T>()

  override fun offer(item: T) {
    items += item
  }

  override fun poll(): T? = items.removeLastOrNull()
}

/**
 * [PriorityQueue] is a [Queue] implementation where items are ordered according to the lowest
 * [PriorityQueue.Item.weighs].
 */
internal class PriorityQueue<T : Any> : Queue<PriorityQueue.Item<T>> {

  /**
   * [items] stores the [PriorityQueue.Item] instances in the natural order according to
   * [PriorityQueue.Item.weighs].
   */
  private val items = mutableListOf<Item<T>>()

  /** Insert the [item] into [items] at the index according to the [PriorityQueue.Item.weighs]. */
  override fun offer(item: Item<T>) =
      when (val index = items.indexOfFirst { item.priority < it.priority }) {
        -1 -> items += item
        else -> items.add(index, item)
      }

  /** Retrieve and remove the item with the lowest [PriorityQueue.Item.weighs]. */
  override fun poll(): Item<T>? = items.removeFirstOrNull()

  /**
   * Set the [priority] of the [PriorityQueue.Item] with the [value].
   *
   * The [value] **must** be present in [items], otherwise an [IllegalStateException] is thrown.
   */
  operator fun set(value: T, priority: Float) {
    val item = checkNotNull(items.find { it.value == value })
    val index = items.indexOf(item)
    items[index] = item.copy(priority = priority)
    items.sortBy { it.priority }
  }

  /** [PriorityQueue.Item] simply relates a [priority] to a [value]. */
  data class Item<T : Any>(val value: T, val priority: Float)
}

/** Convert the vertex [V] and [weight] to a [PriorityQueue.Item]. */
internal infix fun <V : Any> V.weighs(weight: Int): PriorityQueue.Item<V> =
    this weighs weight.toFloat()

/** Convert the vertex [V] and [priority] to a [PriorityQueue.Item]. */
internal infix fun <V : Any> V.weighs(priority: Float): PriorityQueue.Item<V> =
    PriorityQueue.Item(this, priority)
