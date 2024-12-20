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

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

/**
 * Build a [Graph] instance.
 *
 * The characteristics of the [Graph] are customizable through the [features].
 *
 * The [builder] function is used to populate the [Graph] with vertices and edges.
 *
 * @param V the type of each vertex in the graph
 * @param E the type of each edge label in the graph
 * @param features the graph features
 * @param builder the function adding vertices and edges to the [Graph]
 * @return the graph
 */
@OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
fun <V : Any, E : Edge<V>> buildGraph(
    vararg features: Feature = emptyArray(),
    @BuilderInference builder: GraphBuilder<V, E>.() -> Unit,
): Graph<V, E> {
  contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
  return InMemoryGraph<V, E>(features.toSet()).apply(builder)
}

/**
 * [GraphBuilder] enables the addition of vertices and edges to a [Graph].
 * > Currently, a [Graph] cannot be a [Multigraph](https://en.wikipedia.org/wiki/Multigraph),
 * > therefore any attempt to [add] a parallel edge will result in a [EdgeAlreadyExists] exception.
 *
 * @param V the type of each vertex
 * @param E the type of each edge
 */
interface GraphBuilder<V : Any, E : Edge<V>> : EdgeDsl<V> {

  /**
   * Add the vertices and the [edge] to the graph.
   *
   * @param edge the [Edge], including the [Edge.source] and [Edge.target] vertices, to add
   * @return `this` graph builder
   * @throws EdgeAlreadyExists if an edge between the [Edge.source] and [Edge.target] already exists
   * @throws LoopException if the [Edge.source] and [Edge.target] are equal
   * @throws AcyclicException if an edge between the [Edge.source] and [Edge.target] creates a cycle
   */
  fun add(edge: E): GraphBuilder<V, E>

  /** [plusAssign] is an operator alias for [GraphBuilder.add]. */
  operator fun <V : Any, E : Edge<V>> GraphBuilder<V, E>.plusAssign(edge: E) {
    add(edge)
  }

  /**
   * Add the vertices and the [edges] to the graph.
   *
   * @param edges the [Iterable] of [Edge] instances to add
   * @return `this` graph builder
   * @throws EdgeAlreadyExists if any of the [edges] already exist
   * @throws LoopException if any of the [Edge.source] and [Edge.target] are equal
   * @throws AcyclicException if an edge between the [Edge.source] and [Edge.target] creates a cycle
   */
  fun addAll(edges: Iterable<E>): GraphBuilder<V, E> =
      edges.fold(this) { builder, edge -> builder.add(edge) }

  /** [plusAssign] is an operator alias for [GraphBuilder.addAll]. */
  operator fun <V : Any, E : Edge<V>> GraphBuilder<V, E>.plusAssign(edges: Iterable<E>) {
    addAll(edges)
  }
}
