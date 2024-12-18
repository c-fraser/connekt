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

/**
 * [TraversalStrategy] determines the order in which the vertices in a [Graph] are traversed.
 *
 * The traversal will begin at the [vertex]. If the graph is directed or contains isolated
 * subgraphs, all the vertices in the graph may not be traversed, depending on the edges connected
 * to the [vertex].
 *
 * @param V the type of each vertex in the [Graph]
 * @property vertex the vertex to begin traversing from
 */
sealed class TraversalStrategy<V : Any>(val vertex: V)

/**
 * [DepthFirst] represents a [depth-first search](https://en.wikipedia.org/wiki/Depth-first_search)
 * of the [Graph], beginning at the [vertex].
 */
class DepthFirst<V : Any>(vertex: V) : TraversalStrategy<V>(vertex)

/**
 * [BreadthFirst] represents a
 * [breadth-first search](https://en.wikipedia.org/wiki/Breadth-first_search) of the [Graph],
 * beginning at the [vertex].
 */
class BreadthFirst<V : Any>(vertex: V) : TraversalStrategy<V>(vertex)

/** Initialize and return a [Queue] for traversing a [Graph] in the appropriate order. */
internal fun <V : Any> TraversalStrategy<V>.queue(): Queue<V> =
    when (this) {
      is DepthFirst<V> -> LIFOQueue()
      is BreadthFirst<V> -> FIFOQueue()
    }
