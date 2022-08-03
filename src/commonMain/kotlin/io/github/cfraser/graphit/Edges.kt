package io.github.cfraser.graphit

/**
 * An [Edge] connects [source] and [target] vertices in a [Graph].
 *
 * @param V the type of each vertex in the [Graph]
 */
sealed interface Edge<V : Any> {

  /** [source] is the vertex in the [Graph] that the [Edge] originates from. */
  val source: V

  /** [target] is the vertex in the [Graph] that the [Edge] connects to. */
  val target: V
}

/**
 * [Weighted] specifies that the edges in a [Graph] are
 * [weighted](https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)#Weighted_graph).
 */
sealed interface Weighted {

  /** The [weight] of the edge. */
  val weight: Int
}

/**
 * [BasicEdge] is an [Edge] implementation that only stores the [source] and [target] vertices.
 *
 * @property source the [Edge.source]
 * @property target the [Edge.target]
 */
data class BasicEdge<V : Any>(override val source: V, override val target: V) : Edge<V> {

  override fun toString() = "$source -> $target"
}

/**
 * [WeightedEdge] is an [Edge] implementation that captures a [weight] for the [source] to [target]
 * connection.
 *
 * @property source the [Edge.source]
 * @property target the [Edge.target]
 * @property weight the weight of the edge
 */
data class WeightedEdge<V : Any>(
    override val source: V,
    override val target: V,
    override val weight: Int
) : Edge<V>, Weighted {

  override fun toString() = "$source -[$weight]-> $target"
}

/**
 * [GenericEdge] is an [Edge] implementation which enables the storage of arbitrary [attributes], in
 * addition to the [source] and [target] vertices.
 *
 * @param T the type of [attributes] stored in each edge in the [Graph]
 * @property source the [Edge.source]
 * @property target the [Edge.target]
 * @property attributes the edge attributes
 */
data class GenericEdge<V : Any, T : Any>(
    override val source: V,
    override val target: V,
    val attributes: T
) : Edge<V> {

  override fun toString() = "$source -($attributes)-> $target"
}

/**
 * [WeightedGenericEdge] is an [Edge] implementation that is a combination of [WeightedEdge] and
 * [GenericEdge].
 *
 * @param T the type of [attributes] stored in each edge in the [Graph]
 * @property source the [Edge.source]
 * @property target the [Edge.target]
 * @property weight the weight of the edge
 * @property attributes the edge attributes
 */
data class WeightedGenericEdge<V : Any, T : Any>(
    override val source: V,
    override val target: V,
    override val weight: Int,
    val attributes: T
) : Edge<V>, Weighted {

  override fun toString() = "$source -[$weight]-($attributes)-> $target"
}

/**
 * [EdgeDsl] empowers the fluent construction of [Edge] types.
 *
 * @param V the type of each vertex
 */
interface EdgeDsl<V : Any> {

  /**
   * Initialize a [BasicEdge] from the vertices.
   *
   * > The receiver `this` is the source and [vertex] is the target.
   *
   * @param vertex the target vertices
   * @return the edge
   */
  infix fun V.to(vertex: V): BasicEdge<V> = BasicEdge(this, vertex)

  /**
   * Convert the [BasicEdge] to a [WeightedEdge] using the [BasicEdge.source], [BasicEdge.target],
   * and [weight].
   *
   * @param weight the weight of the edge
   * @return the edge
   */
  infix fun BasicEdge<V>.weighs(weight: Int): WeightedEdge<V> = WeightedEdge(source, target, weight)

  /**
   * Convert the [BasicEdge] to a [GenericEdge] using the [BasicEdge.source], [BasicEdge.target],
   * and [attributes].
   *
   * @param attributes the edge attributes
   * @return the edge
   */
  infix fun <T : Any> BasicEdge<V>.with(attributes: T): GenericEdge<V, T> =
      GenericEdge(source, target, attributes)

  /**
   * Convert the [WeightedEdge] to a [WeightedGenericEdge] using the [WeightedEdge.source],
   * [WeightedEdge.target], [WeightedEdge.weight], and [attributes].
   *
   * @param attributes the edge attributes
   * @return the edge
   */
  infix fun <T : Any> WeightedEdge<V>.with(attributes: T): WeightedGenericEdge<V, T> =
      WeightedGenericEdge(source, target, weight, attributes)
}
