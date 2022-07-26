package io.github.cfraser.graphit

/**
 * [Graph] is a generic immutable graph data structure consisting of vertices and edges.
 *
 * @param V the type of each vertex
 * @param E the type of each edge label
 */
abstract class Graph<V : Any, E : Any> @PublishedApi internal constructor() {

  /**
   * Check whether the [vertex] is contained within the graph.
   *
   * @param vertex the vertex to check
   * @return `true` if the vertex is in the graph, otherwise `false`
   */
  abstract fun containsVertex(vertex: V): Boolean

  /** [contains] is an operator alias for [containsVertex]. */
  operator fun contains(vertex: V) = containsVertex(vertex)

  /**
   * Check whether an edge connecting the [vertices] is contained within the graph.
   *
   * @param vertices the [Vertices] to check
   * @return `true` if the edge is in the graph, otherwise `false`
   */
  abstract fun containsEdge(vertices: Vertices<V>): Boolean

  /** [contains] is an operator alias for [containsEdge]. */
  operator fun contains(vertices: Vertices<V>) = containsEdge(vertices)

  /**
   * Get the edges connected to the [vertex].
   *
   * @param vertex the vertex
   * @throws VertexNotFound if [vertex] isn't found in the graph
   * @return the [Collection] of edges connected to the vertex
   */
  abstract fun getEdges(vertex: V): Collection<Edge<V, E>>

  /** [get] is an operator alias for [getEdges]. */
  operator fun get(vertex: V) = getEdges(vertex)

  /**
   * Get the edge between the [vertices].
   *
   * If the graph is undirected, then the directionality of the [Vertices] is ignored. Consequently,
   * the values in the returned [Edge.source] and [Edge.target] may be swapped, respective to the
   * [Vertices].
   *
   * @param vertices the source and target [Vertices]
   * @throws VertexNotFound if either of the [Vertices] isn't found in the graph
   * @throws EdgeNotFound if an edge between the [Vertices] is not found
   * @return the [Edge] between the vertices
   */
  abstract fun getEdge(vertices: Vertices<V>): Edge<V, E>

  /** [get] is an operator alias for [getEdges]. */
  operator fun get(vertices: Vertices<V>) = getEdge(vertices)

  /**
   * Traverse the graph according to the [traversal] order, beginning at the [vertex].
   *
   * The beginning [vertex] is optional. If the provided [vertex] is `null`, then the traversal will
   * begin at an arbitrary vertex.
   *
   * @param vertex the vertex to begin searching from
   * @return an [Iterable] which traverses the graph
   */
  abstract fun traverse(traversal: Traversal, vertex: V? = null): Iterable<V>

  /**
   * Compute the shortest path between the [vertices].
   *
   * > The shortest path computation runs in `O(|V|+|E|log(|V|))` time.
   *
   * If the graph is weighted, then the [Edge.weight] is considered in the path calculation.
   *
   * If there are multiple equivalent shortest paths, then the returned path will be arbitrary
   * selected.
   *
   * @param vertices the source and target vertices
   * @throws VertexNotFound if either of the [vertices] isn't found in the graph
   * @throws NoPathExists if no path between [vertices] exists
   * @return the shortest path, including the given [vertices]
   */
  abstract fun shortestPath(vertices: Vertices<V>): Collection<V>

  /**
   * Calculate the [degree](https://en.wikipedia.org/wiki/Degree_(graph_theory)) of the [vertex].
   *
   * @param vertex the vertex to get the degree of
   * @throws [VertexNotFound] if the [vertex] isn't found in the graph
   * @return the degree of the vertex
   */
  abstract fun degree(vertex: V): Int

  /**
   * Return the
   * [strongly connected components](https://en.wikipedia.org/wiki/Strongly_connected_component)
   * within the graph.
   *
   * > [strongConnections] is only applicable for directed graphs.
   *
   * @throws UndirectedException if the graph is undirected
   * @return a paths of the strongly connected vertices
   */
  abstract fun strongConnections(): Collection<Collection<V>>

  /**
   * Return an adjacency map representing all the connections in the graph.
   *
   * The entries in the returned map associate each vertex in the graph with the adjacent vertices
   * and the connecting edge.
   *
   * For example, a graph with edges AB and AC, the adjacency map would be...
   *
   * ```kotlin
   * mapOf("A" to mapOf("B" to Edge("A", "B"), "C" to Edge("A", "C")))
   * ```
   *
   * @return the adjacency map
   */
  abstract fun adjacencies(): Map<V, Map<V, Edge<V, E>>>

  /**
   * Determines whether an [Edge] between the [Vertices] would introduce a cycle.
   *
   * @param vertices the source and target vertices
   * @throws VertexNotFound if either of the [vertices] isn't found in the graph
   * @return `true` if a cycle is introduced, otherwise `false`
   */
  abstract fun isCyclical(vertices: Vertices<V>): Boolean

  /**
   * Returns an ASCII diagram of the graph.
   *
   * @return the graph diagram
   */
  override fun toString(): String {
    return TODO()
  }
}
