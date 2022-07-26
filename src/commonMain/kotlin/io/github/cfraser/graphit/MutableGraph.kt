package io.github.cfraser.graphit

/**
 * [MutableGraph] is a [Graph] that enables the addition of vertices and edges.
 *
 * @param V the type of each vertex
 * @param E the type of each edge label
 */
abstract class MutableGraph<V : Any, E : Any> @PublishedApi internal constructor() : Graph<V, E>() {

  /**
   * Add the [vertex] to the graph.
   *
   * If the [vertex] already exists, then it is overwritten by the new vertex with the provided
   * [vertex].
   *
   * @param vertex the [V] of the vertex
   */
  abstract fun addVertex(vertex: V)

  /** [unaryPlus] is an operator alias for [addVertex]. */
  operator fun V.unaryPlus() = addVertex(this)

  /**
   * Add the [edge] to the graph.
   *
   * If the graph is weighted and/or labeled, then a [Edge.weight] and/or [Edge.label] must be
   * provided, otherwise, the [Edge.weight] and/or [Edge.label] must be `null`.
   *
   * If the graph is directed, then the resulting edge is also directed.
   *
   * @param edge the [Edge] to add
   * @throws VertexNotFound if the [Edge.source] or [Edge.target] isn't found in the graph
   * @throws EdgeAlreadyExists if an edge between the [Edge.source] and [Edge.target] already exists
   * @throws WeightedException if the graph is weighted but a `null` [Edge.weight] is specified
   * @throws UnweightedException if the graph is unweighted but a [Edge.weight] is specified
   * @throws LabeledException if the graph is labeled but a `null` [Edge.label] is specified
   * @throws UnlabeledException if the graph is unlabeled but a [Edge.label] is specified
   */
  abstract fun addEdge(edge: Edge<V, E>)

  /** [unaryPlus] is an operator alias for [addEdge]. */
  operator fun Edge<V, E>.unaryPlus() = addEdge(this)

  /** [unaryPlus] is an operator alias for [addEdge]. */
  operator fun Vertices<V>.unaryPlus() = addEdge(Edge(first, second))
}
