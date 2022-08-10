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
package io.github.cfraser.graphit

/**
 * [Graph] is a generic immutable graph data structure consisting of vertices and edges.
 *
 * A [Graph] can be [directed](https://en.wikipedia.org/wiki/Directed_graph) and/or
 * [acyclic](https://en.wikipedia.org/wiki/Acyclic_graph), as specified by its [features].
 *
 * A [Graph] may contain [isolated subgraphs](https://en.wikipedia.org/wiki/Component_(graph_theory)
 * ). Consequently, [traverse] may not return all the vertices in the [Graph], depending on the
 * [TraversalStrategy.vertex].
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
   * @throws VertexNotFound if the [TraversalStrategy.vertex] isn't found in the graph
   * @return an [Iterable] which traverses the graph
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
   * Returns the [DOT language](https://graphviz.org/doc/info/lang.html) representation of the
   * graph.
   *
   * The returned data enables a visualization of the graph to be generated via
   * [Graphviz](https://graphviz.org/), as shown below.
   *
   * ```bash
   * # generate an SVG from the output of `Graph.toString()`
   * echo "$GRAPH_DOT" | dot -Tsvg > graph.svg
   * ```
   *
   * @return the graph data
   */
  override fun toString(): String
}

/**
 * [InMemoryGraph] is a [Graph] and [GraphBuilder] implementation which stores the [vertices] and
 * edges within in-memory data structures.
 */
internal class InMemoryGraph<V : Any, E : Edge<V>>(override val features: Set<Feature>) :
    GraphBuilder<V, E>, Graph<V, E> {

  private val vertices: MutableSet<V> = mutableSetOf()
  private val outEdges: MutableMap<V, MutableMap<V, E>> = mutableMapOf()
  private val inEdges: MutableMap<V, MutableMap<V, E>> = mutableMapOf()

  override fun add(edge: E): GraphBuilder<V, E> = apply {
    if (edge.source == edge.target) throw LoopException
    edge(edge.source, edge.target)?.also { throw EdgeAlreadyExists(edge) }
    if (isAcyclic && isCyclic(edge)) throw AcyclicException(edge)

    vertices += edge.source
    vertices += edge.target

    fun MutableMap<V, MutableMap<V, E>>.put(source: V, target: V, edge: E) {
      getOrPut(source) { mutableMapOf() }[target] = edge
    }

    outEdges.put(edge.source, edge.target, edge)
    inEdges.put(edge.target, edge.source, edge)
    if (isUndirected) {
      outEdges.put(edge.target, edge.source, edge)
      inEdges.put(edge.source, edge.target, edge)
    }
  }

  override fun contains(vertex: V): Boolean = vertex in vertices

  override fun contains(vertices: Vertices<V>): Boolean =
      try {
        this[vertices].let { true }
      } catch (_: EdgeNotFound) {
        false
      }

  override fun get(vertex: V): Collection<E> =
      vertex
          .exists()
          .let {
            fun MutableMap<V, MutableMap<V, E>>.edges() = this[it]?.values.orEmpty()
            outEdges.edges() + inEdges.edges()
          }
          .toSet()

  override fun get(vertices: Vertices<V>): E {
    val (source, target) = vertices.exists()
    return edge(source, target) ?: throw EdgeNotFound(source, target)
  }

  override fun traverse(strategy: TraversalStrategy<V>): Iterable<V> {
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

  override fun shortestPath(vertices: Vertices<V>): Collection<V> {
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
      val edges = edges(vertex) ?: continue
      val isFinite = checkNotNull(weights[vertex]).isFinite()
      for ((successor, edge) in edges) {
        val weight = checkNotNull(weights[vertex]) + (edge.weight()?.toFloat() ?: 0f)
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

  override fun toString(): String {
    val (graphType, edgeOperator) = if (isDirected) "digraph" to "->" else "graph" to "--"
    data class Statement(val source: V, val target: V, val weight: Int?, val attributes: Any?)
    val statements =
        vertices
            .flatMap { source ->
              edges(source).orEmpty().map { (target, edge) ->
                Statement(source, target, edge.weight(), edge.attributes())
              }
            }
            .distinctBy { setOf(it.source, it.target) }
            .map { (source, target, weight, attributes) ->
              "$source $edgeOperator $target${when {
                weight != null && attributes != null -> 
                  "[weight=$weight, label=\"weight: $weight, attributes: $attributes\"]"
                weight != null -> "[weight=$weight, label=$weight]"
                attributes != null -> "[label=\"$attributes\"]"
                else -> null
              }?.let { " $it" }.orEmpty()};"
            }
    return StringBuilder()
        .appendLine("strict $graphType {")
        .run { statements.fold(this) { builder, statement -> builder.appendLine(statement) } }
        .append("}")
        .toString()
  }

  /**
   * Get the edge between the [source] and [target] vertices, if it exists.
   *
   * > [edge] **doesn't** verify the existence of [source] and [target].
   */
  private fun edge(source: V, target: V): E? =
      outEdges[source]?.let { it[target] }
          ?: inEdges[target]?.let { it[source] }
              ?: takeIf { isUndirected }
              ?.run { outEdges[target]?.let { it[source] } ?: inEdges[source]?.let { it[target] } }

  /**
   * Get the edges connected to the [vertex].
   *
   * > [edges] **doesn't** verify the existence of [vertex].
   *
   * The returned [MutableMap] maps the target vertex to the connecting edge.
   */
  private fun edges(vertex: V): MutableMap<V, E>? =
      if (isDirected) outEdges[vertex]
      else
          (outEdges[vertex].orEmpty() + inEdges[vertex].orEmpty())
              .takeUnless { it.isEmpty() }
              ?.toMutableMap()

  /**
   * Get the vertices adjacent to [V].
   *
   * > [adjacentVertices] **doesn't** verify the existence of [V].
   */
  private fun V.adjacentVertices(): Collection<V> = let {
    fun MutableMap<V, MutableMap<V, E>>.vertices() = this[it]?.keys.orEmpty()
    return outEdges.vertices() + takeIf { isUndirected }?.run { inEdges.vertices() }.orEmpty()
  }

  /**
   * Check if adding the [edge] would introduce a cycle in the graph.
   *
   * If the [Edge.source] and [Edge.target] vertices exist, then [isCyclic] traverses the connected
   * edges to verify the graph is cyclic or acyclic.
   *
   * @return `true` if the graph would be cyclic with the [edge], otherwise `false`
   */
  private fun isCyclic(edge: E): Boolean {
    if (edge.source in this && edge.target in this) {
      val vertices = LIFOQueue<V>().apply { offer(edge.source) }
      val visited = mutableMapOf<V, Boolean>()
      while (true) {
        val vertex = vertices.poll() ?: break
        if (vertex == edge.target) return true
        visited[vertex] = true
        inEdges[vertex]?.keys?.filter { it !in visited }?.forEach(vertices::offer)
      }
    }

    return false
  }

  /**
   * Check that `this` vertex exist.
   *
   * @throws VertexNotFound if the vertex is not found
   * @return `this` vertex
   */
  private fun V.exists(): V = also { if (it !in vertices) throw VertexNotFound(it) }

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
