package io.github.cfraser.graphit

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
  ACYCLIC,

  /**
   * Specifies ths edges in the graph are
   * [weighted](https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)#Weighted_graph).
   */
  WEIGHTED,

  /**
   * Specifies the edges in the graph are [labeled](https://en.wikipedia.org/wiki/Graph_labeling).
   */
  LABELED
}
