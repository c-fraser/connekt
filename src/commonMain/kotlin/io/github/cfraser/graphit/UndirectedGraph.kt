package io.github.cfraser.graphit

/**
 * [UndirectedGraph] is a [GraphBuilder] and [Graph] implementation for an
 * [undirected graph](https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)#Graph).
 */
internal class UndirectedGraph<V : Any, E : Edge<V>>(features: Array<out Feature>) :
    GraphBuilder<V, E>, Graph<V, E> {

  override val features = features.toSet()

  private val vertexSet: MutableSet<V> = HashSet()
  private val edgeMap: MutableMap<V, MutableMap<V, E>> = HashMap()

  override fun add(edge: E) = apply {
    val (source, target) = edge.vertices()
    if (source == target) throw LoopException
    vertexSet += source
    vertexSet += target

    try {
      undirectedEdge(source, target)
      throw EdgeAlreadyExists(source, target)
    } catch (_: EdgeNotFound) {}

    if (isAcyclic) checkAcyclic(source, target)

    edgeMap.getOrPut(source, ::HashMap)[target] = edge
    edgeMap.getOrPut(target, ::HashMap)[source] = edge
  }

  override fun contains(vertex: V) = vertex in vertexSet

  override fun contains(vertices: Vertices<V>) =
      try {
        this[vertices]
        true
      } catch (_: EdgeNotFound) {
        false
      }

  override fun get(vertex: V) = vertex.exists().let { edgeMap[it]?.values }?.toHashSet().orEmpty()

  override fun get(vertices: Vertices<V>): E =
      vertices.exists().let { (source, target) -> undirectedEdge(source, target) }

  override fun traverse(strategy: TraversalStrategy<V>) = buildSet {
    val vertices = strategy.queue()
    val visited = mutableMapOf<V, Boolean>()

    vertices.offer(strategy.vertex?.exists() ?: vertexSet.random())

    while (true) {
      val vertex = vertices.poll() ?: break
      this += vertex
      visited[vertex] = true
      vertex.adjacencies().filter { it !in visited }.forEach(vertices::offer)
    }
  }

  override fun shortestPath(vertices: Vertices<V>) = buildList {
    val (source, target) = vertices.exists()

    val visited = mutableMapOf<V, Boolean>()
    val weights = mutableMapOf<V, Float>()
    val queue = PriorityQueue<V>()

    for (vertex in vertexSet) queue.offer(
        vertex weighs
            when (vertex) {
              source -> {
                visited[source] = true
                0f.also { weights[source] = it }
              }
              else -> {
                visited[vertex] = false
                Float.POSITIVE_INFINITY.also { weights[vertex] = it }
              }
            })

    val predecessors = mutableMapOf<V, V>()
    while (true) {
      val item = queue.poll() ?: break
      val (vertex, _) = item
      if (vertex == target && vertex !in edgeMap) throw NoPathExists(source, target)
      val edges = edgeMap[vertex] ?: continue
      val isFinite = checkNotNull(weights[vertex]).isFinite()
      for ((successor, edge) in edges) {
        val weight =
            checkNotNull(weights[vertex]) +
                when (edge) {
                  is Weighted -> edge.weight.toFloat()
                  else -> 1f
                }
        if (weight < checkNotNull(weights[successor]) && isFinite) {
          weights[successor] = weight
          predecessors[successor] = vertex
          queue[successor] = weight
        }
      }
    }

    // Backtrack the predecessors from target to source
    var path = target
    while (path != source) {
      this += path
      path = checkNotNull(predecessors[path])
    }
  }

  override fun toString(): String {
    TODO("Not yet implemented")
  }

  /**
   * Get the edge from [source] to [target], or [target] to [source], by retrieving from [edgeMap].
   */
  private fun undirectedEdge(source: V, target: V) =
      edgeMap[source]?.let { it[target] }
          ?: edgeMap[target]?.let { it[source] } ?: throw EdgeNotFound(source, target)

  /**
   * Throw an [AcyclicException] if an edge between the [source] and [target] vertices would not
   * introduce a cycle.
   */
  private fun checkAcyclic(source: V, target: V) {
    traverse(DepthFirst(source)).forEach {
      // If the current vertex, i.e. a predecessor of the source vertex, is also the target vertex,
      // an edge between these two would create a cycle.
      if (it == target) throw AcyclicException(source, target)
    }
  }

  /** Return vertices adjacent to [V]. */
  private fun V.adjacencies(): Collection<V> = edgeMap[this]?.keys?.toHashSet().orEmpty()

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

  /**
   * Check that `this` vertex exist.
   *
   * @throws VertexNotFound if the vertex is not found
   * @return `this` vertex
   */
  private fun V.exists(): V = also { if (it !in vertexSet) throw VertexNotFound(it) }
}
