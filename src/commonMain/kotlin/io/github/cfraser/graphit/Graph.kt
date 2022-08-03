package io.github.cfraser.graphit

/**
 * [Graph] is a generic immutable graph data structure consisting of vertices and edges.
 *
 * @param V the type of each vertex
 * @param E the type of each edge
 */
interface Graph<V : Any, E : Edge<V>> {

  /** The [features] the [Graph] was built with. */
  val features: Set<Feature>

  /**
   * Check whether the [vertex] is contained within the graph.
   *
   * @param vertex the vertex to check
   * @return `true` if the vertex is in the graph, otherwise `false`
   */
  operator fun contains(vertex: V): Boolean

  /**
   * Check whether an edge connecting the [vertices] is contained within the graph.
   *
   * @param vertices the [Vertices] to check
   * @throws VertexNotFound if either of the [vertices] isn't found in the graph
   * @return `true` if the edge is in the graph, otherwise `false`
   */
  operator fun contains(vertices: Vertices<V>): Boolean

  /**
   * Get the edges connected to the [vertex].
   *
   * @param vertex the vertex
   * @throws VertexNotFound if [vertex] isn't found in the graph
   * @return the [Collection] of edges connected to the vertex
   */
  operator fun get(vertex: V): Collection<E>

  /**
   * Get the edge between the [vertices].
   *
   * If the graph is undirected, then the directionality of the [vertices] is ignored. Consequently,
   * the values in the returned [Edge.source] and [Edge.target] may be swapped, respective to the
   * [Vertices].
   *
   * @param vertices the source and target [Vertices]
   * @throws VertexNotFound if either of the [vertices] isn't found in the graph
   * @throws EdgeNotFound if an edge between the [vertices] is not found
   * @return the [Edge] between the vertices
   */
  operator fun get(vertices: Vertices<V>): E

  /**
   * Traverse the vertices in the graph.
   *
   * The returned [Iterable] is ordered according to the [strategy].
   *
   * @param strategy the [TraversalStrategy] which specifies how to traverse the graph
   * @return an [Iterable] which traverses the graph
   * @throws VertexNotFound if the [TraversalStrategy.vertex] isn't found in the graph
   */
  fun traverse(strategy: TraversalStrategy<V> = DepthFirst(null)): Iterable<V>

  /**
   * Compute the shortest path between the [vertices].
   *
   * > The shortest path computation runs in `O(V+E*log(V))` time.
   *
   * If the graph is weighted, then the [WeightedEdge.weight] is considered in the path calculation.
   *
   * If there are multiple equivalent shortest paths, then the returned path will be arbitrary
   * selected.
   *
   * @param vertices the source and target vertices
   * @throws VertexNotFound if either of the [vertices] isn't found in the graph
   * @throws NoPathExists if no path between [vertices] exists
   * @throws IllegalStateException if the shortest path computation fails
   * @return the shortest path, including the given [vertices]
   */
  fun shortestPath(vertices: Vertices<V>): Collection<V>

  /**
   * Returns a diagram of the graph.
   *
   * @return the graph diagram
   */
  override fun toString(): String
}
