package io.github.cfraser.graphit

/** [Traversal] enumerates the orders in which the vertices in a [Graph] can be traversed. */
enum class Traversal {

  /**
   * Specifies a [depth-first search](https://en.wikipedia.org/wiki/Depth-first_search) of the
   * graph.
   */
  DEPTH_FIRST,

  /**
   * Specifies a [breadth-first search](https://en.wikipedia.org/wiki/Breadth-first_search) of the
   * graph.
   */
  BREADTH_FIRST
}
