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
  fun traverse(strategy: TraversalStrategy<V>): Iterable<V>

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

/**
 * [BaseGraph] is an abstract class that consolidates common functionality for [Graph]
 * implementations.
 *
 * The usage of [VertexSetInitializer] and [EdgeMapInitializer] flexibility regarding the storage of
 * vertices and edges in the graph. By default, the vertices and edges are stored in-memory, but
 * external/distributed is possible by providing a customized [VertexSetInitializer] and/or
 * [EdgeMapInitializer].
 */
internal abstract class BaseGraph<V : Any, E : Edge<V>>(
    features: Array<out Feature>,
    vertexSetInitializer: VertexSetInitializer<V> = VertexSetInitializer(::HashSet),
    protected val edgeMapInitializer: EdgeMapInitializer<V, E> =
        object : EdgeMapInitializer<V, E> {
          override fun sourceMap(): MutableMap<V, MutableMap<V, E>> = HashMap()
          override fun targetMap(): MutableMap<V, E> = HashMap()
        }
) : GraphBuilder<V, E>, Graph<V, E> {

  final override val features = features.toSet()

  private val vertices = vertexSetInitializer.vertexSet()

  final override fun add(edge: E): GraphBuilder<V, E> = apply {
    if (edge.source == edge.target) throw LoopException
    getEdge(edge.source, edge.target)?.also { throw EdgeAlreadyExists(edge) }
    if (isAcyclic && isCyclic(edge)) throw AcyclicException(edge)
    vertices += edge.source
    vertices += edge.target
    addEdge(edge)
  }

  final override fun contains(vertex: V): Boolean = vertex in vertices

  final override fun contains(vertices: Vertices<V>): Boolean =
      try {
        this[vertices].let { true }
      } catch (_: EdgeNotFound) {
        false
      }

  final override fun get(vertices: Vertices<V>): E {
    val (source, target) = vertices.exists()
    return getEdge(source, target) ?: throw EdgeNotFound(source, target)
  }

  final override fun traverse(strategy: TraversalStrategy<V>): Iterable<V> {
    val vertices = strategy.queue()
    val visited = mutableMapOf<V, Boolean>()

    vertices.offer(strategy.vertex.exists())

    return buildList {
      while (true) {
        val vertex = vertices.poll() ?: break
        this += vertex
        visited[vertex] = true
        vertex.adjacentVertices().filter { it !in visited }.forEach(vertices::offer)
      }
    }
  }

  final override fun shortestPath(vertices: Vertices<V>): Collection<V> {
    val (source, target) = vertices.exists()

    val weights = mutableMapOf<V, Float>()
    val queue = PriorityQueue<V>()
    for (vertex in this.vertices) {
      val weight = if (vertex == source) 0f else Float.POSITIVE_INFINITY
      weights[vertex] = weight
      queue.offer(vertex weighs weight)
    }

    val predecessors = mutableMapOf<V, V>()
    while (true) {
      val vertex = queue.poll()?.value ?: break
      val edges = getEdges(vertex) ?: continue
      val isFinite = checkNotNull(weights[vertex]).isFinite()
      for ((successor, edge) in edges) {
        val weight =
            checkNotNull(weights[vertex]) +
                when (edge) {
                  is Weighted -> edge.weight.toFloat()
                  else -> 0f
                }
        if (weight < checkNotNull(weights[successor]) && isFinite) {
          weights[successor] = weight
          queue[successor] = weight
          predecessors[successor] = vertex
        }
      }
    }

    return buildList {
          // Backtrack the predecessors from target to source
          var path = target
          while (path != source) {
            this += path
            path = predecessors[path] ?: throw NoPathExists(source, target)
          }
          this += source
        }
        .reversed()
  }

  final override fun toString(): String {
    return super.toString()
  }

  /**
   * Get the edge between the [source] and [target] vertices, if it exists.
   *
   * > Implementations of [getEdge] **shouldn't** verify the existence of [source] and [target].
   */
  protected abstract fun getEdge(source: V, target: V): E?

  /**
   * Get the edges connected to the [vertex].
   *
   * > Implementations of [getEdges] **shouldn't** verify the existence of [vertex].
   *
   * The returned [MutableMap] maps the connecting edge to the target vertex.
   */
  protected abstract fun getEdges(vertex: V): MutableMap<V, E>?

  /**
   * Add the [edge] between the [Edge.source] and [Edge.target] vertices.
   *
   * > Implementations of [addEdge] are **only** responsible for the storage of the [edge]. The
   * validity of the [edge] is checked prior to the invocation of [addEdge].
   */
  protected abstract fun addEdge(edge: E)

  /**
   * Get the vertices adjacent to [V].
   *
   * > Implementations of [adjacentVertices] **shouldn't** verify the existence of [V].
   */
  protected abstract fun V.adjacentVertices(): Collection<V>

  /**
   * Check that `this` vertex exist.
   *
   * @throws VertexNotFound if the vertex is not found
   * @return `this` vertex
   */
  protected fun V.exists(): V = also { if (it !in vertices) throw VertexNotFound(it) }

  /**
   * Check if adding the [edge] would introduce a cycle in the graph.
   *
   * If the [Edge.source] and [Edge.target] vertices exist, then [isCyclic] traverses the connected
   * edges to verify the graph is cyclic or acyclic.
   *
   * @return `true` if the graph would be cyclic with the [edge], otherwise `false`
   */
  private fun isCyclic(edge: E): Boolean {
    if (edge.source !in this || edge.target !in this) return false

    val vertices = LIFOQueue<V>().apply { offer(edge.source) }
    val visited = mutableMapOf<V, Boolean>()
    while (true) {
      val vertex = vertices.poll() ?: break
      if (vertex == edge.target) return true
      visited[vertex] = true
      this[vertex]
          .flatMap { listOf(it.source, it.target) }
          .filter { it !in visited }
          .forEach(vertices::offer)
    }

    return false
  }

  /**
   * Check that the source and target [Vertices] exist.
   *
   * @throws VertexNotFound if the vertex is not found
   * @return `this` vertices
   */
  private fun Vertices<V>.exists(): Vertices<V> = apply {
    first.exists()
    second.exists()
  }
}

/**
 * [UndirectedGraph] is a [GraphBuilder] and [Graph] implementation for an
 * [undirected graph](https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)#Graph).
 */
internal class UndirectedGraph<V : Any, E : Edge<V>>(features: Array<out Feature>) :
    BaseGraph<V, E>(features) {

  private val edges = edgeMapInitializer.sourceMap()

  override fun get(vertex: V): Collection<E> =
      vertex.exists().let { edges[it]?.values }?.toSet().orEmpty()

  override fun getEdge(source: V, target: V): E? =
      edges[source]?.let { it[target] } ?: edges[target]?.let { it[source] }

  override fun getEdges(vertex: V): MutableMap<V, E>? = edges[vertex]

  override fun addEdge(edge: E) {
    edges.getOrPut(edge.source, edgeMapInitializer::targetMap)[edge.target] = edge
    edges.getOrPut(edge.target, edgeMapInitializer::targetMap)[edge.source] = edge
  }

  override fun V.adjacentVertices(): Collection<V> = edges[this]?.keys.orEmpty()
}

/**
 * [DirectedGraph] is a [GraphBuilder] and [Graph] implementation for a
 * [directed graph](https://en.wikipedia.org/wiki/Directed_graph).
 */
internal class DirectedGraph<V : Any, E : Edge<V>>(features: Array<out Feature>) :
    BaseGraph<V, E>(features) {

  private val outEdges = edgeMapInitializer.sourceMap()
  private val inEdges = edgeMapInitializer.sourceMap()

  override fun get(vertex: V): Collection<E> =
      vertex
          .exists()
          .let {
            fun MutableMap<V, MutableMap<V, E>>.edges() = this[it]?.values.orEmpty()
            outEdges.edges() + inEdges.edges()
          }
          .toSet()

  override fun getEdge(source: V, target: V): E? =
      outEdges[source]?.let { it[target] } ?: inEdges[target]?.let { it[source] }

  override fun getEdges(vertex: V): MutableMap<V, E>? = outEdges[vertex]

  override fun addEdge(edge: E) {
    outEdges.getOrPut(edge.source, edgeMapInitializer::targetMap)[edge.target] = edge
    inEdges.getOrPut(edge.target, edgeMapInitializer::targetMap)[edge.source] = edge
  }

  override fun V.adjacentVertices(): Collection<V> = let {
    fun MutableMap<V, MutableMap<V, E>>.vertices() = this[it]?.keys.orEmpty()
    return outEdges.vertices() // + inEdgeMap.vertices()
  }
}
