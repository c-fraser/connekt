package io.github.cfraser.graphit

/**
 * [Vertices] is a vertex [Pair] which may, or may not, be connected by an [Edge].
 *
 * In a directed [Graph], the order of the [Vertices] is consequential. Specifically, the
 * [Pair.first] corresponds to the [Edge.source], while [Pair.second] corresponds to the
 * [Edge.target].
 *
 * @param V the type of each vertex in the [Graph]
 */
typealias Vertices<V> = Pair<V, V>

/**
 * [Feature] describes the features supported and enforced by a [Graph]. A [Graph] may have any
 * combination of feature(s) or none.
 */
enum class Feature {

  /**
   * Specifies the edges in the graph are [directed](https://en.wikipedia.org/wiki/Directed_graph).
   */
  DIRECTED,

  /**
   * Specifies the edges in the graph are [acyclic](https://en.wikipedia.org/wiki/Acyclic_graph).
   */
  ACYCLIC
}

/** [isDirected] returns `true` if [Feature.DIRECTED] is in the [Graph.features]. */
internal val Graph<*, *>.isDirected: Boolean
  get() = Feature.DIRECTED in features

/** [isAcyclic] returns `true` if [Feature.ACYCLIC] is in the [Graph.features]. */
internal val Graph<*, *>.isAcyclic: Boolean
  get() = Feature.ACYCLIC in features

/**
 * [TraversalStrategy] determines the order in which the vertices in a [Graph] are traversed.
 *
 * The traversal will begin at the [vertex]. If the graph is directed, all the vertices in the graph
 * may not be traversed, depending on the edges connected to the [vertex].
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

/**
 * [VertexSetInitializer] is a functional interface that has the ability to initialize a
 * [MutableSet] of [V] for storing the vertices in a [Graph].
 */
internal fun interface VertexSetInitializer<V : Any> {

  /** [vertexSet] stores the vertices in a [Graph]. */
  fun vertexSet(): MutableSet<V>
}

/**
 * [EdgeMapInitializer] is a function interface that has the ability to initialize a [MutableMap]
 * for storing the mappings of vertices to edges in a [Graph].
 */
internal interface EdgeMapInitializer<V : Any, E : Edge<V>> {

  /**
   * [sourceMap] stores the edges, a [MutableMap] of target vertices [V] to edge [E], for a source
   * vertex.
   */
  fun sourceMap(): MutableMap<V, MutableMap<V, E>>

  /** [targetMap] stores the mapping of target vertex [V] to edge [E]. */
  fun targetMap(): MutableMap<V, E>
}
