package io.github.cfraser.graphit

/**
 * [DirectedGraph] is a [GraphBuilder] and [Graph] implementation for a
 * [directed graph](https://en.wikipedia.org/wiki/Directed_graph).
 */
class DirectedGraph<V : Any, E : Edge<V>>(features: Array<out Feature>) :
    GraphBuilder<V, E>, Graph<V, E> {

  override val features = features.toSet()

  override fun add(edge: E): GraphBuilder<V, E> {
    TODO("Not yet implemented")
  }

  override fun contains(vertex: V): Boolean {
    TODO("Not yet implemented")
  }

  override fun contains(vertices: Vertices<V>): Boolean {
    TODO("Not yet implemented")
  }

  override fun get(vertex: V): Collection<E> {
    TODO("Not yet implemented")
  }

  override fun get(vertices: Vertices<V>): E {
    TODO("Not yet implemented")
  }

  override fun traverse(strategy: TraversalStrategy<V>): Iterable<V> {
    TODO("Not yet implemented")
  }

  override fun shortestPath(vertices: Vertices<V>): Collection<V> {
    TODO("Not yet implemented")
  }

  override fun toString(): String {
    TODO("Not yet implemented")
  }
}
