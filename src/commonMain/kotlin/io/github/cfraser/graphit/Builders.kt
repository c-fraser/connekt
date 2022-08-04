package io.github.cfraser.graphit

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
    vararg features: Feature = arrayOf(),
    @BuilderInference builder: GraphBuilder<V, E>.() -> Unit,
): Graph<V, E> {
  contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
  return when (Feature.DIRECTED) {
    in features -> DirectedGraph<V, E>(features).apply(builder)
    else -> UndirectedGraph<V, E>(features).apply(builder)
  }
}

/**
 * [GraphBuilder] enables the addition of vertices and edges to a [Graph].
 *
 * @param V the type of each vertex
 * @param E the type of each edge
 */
interface GraphBuilder<V : Any, E : Edge<V>> : EdgeDsl<V> {

  /**
   * Add the vertices and the [edge] to the graph.
   *
   * @param edge the [Edge], including the [Edge.source] and [Edge.target] vertices, to add
   * @throws EdgeAlreadyExists if an edge between the [Edge.source] and [Edge.target] already exists
   * @throws LoopException if the [Edge.source] and [Edge.target] are equal
   * @return `this` graph builder
   */
  fun add(edge: E): GraphBuilder<V, E>

  /**
   * Add the vertices and the [edges] to the graph.
   *
   * @param edges the [Iterable] of [Edge] instances to add
   * @throws EdgeAlreadyExists if any of the [edges] already exist
   * @throws LoopException if any of the [Edge.source] and [Edge.target] are equal
   * @return `this` graph builder
   */
  fun addAll(edges: Iterable<E>): GraphBuilder<V, E> =
      edges.fold(this) { builder, edge -> builder.add(edge) }
}

/** [plusAssign] is an operator alias for [GraphBuilder.add]. */
operator fun <V : Any, E : Edge<V>> GraphBuilder<V, E>.plusAssign(edge: E) {
  add(edge)
}

/** [plusAssign] is an operator alias for [GraphBuilder.addAll]. */
operator fun <V : Any, E : Edge<V>> GraphBuilder<V, E>.plusAssign(edges: Iterable<E>) {
  addAll(edges)
}
