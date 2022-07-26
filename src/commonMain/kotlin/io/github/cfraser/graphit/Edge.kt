package io.github.cfraser.graphit

/**
 * [Edge] is a [Graph] edge connecting [source] and [target] vertices, with an optional [weight] and
 * [label].
 *
 * @param V the type of each vertex in the [Graph]
 * @param E the type of each edge label in the [Graph]
 * @property source the source vertex
 * @property target the target vertex
 * @property weight the weight of the edge
 * @property label the label of the edge
 */
data class Edge<out V : Any, out E : Any>(
    val source: V,
    val target: V,
    val weight: Int? = null,
    val label: E? = null
) {

  override fun toString() =
      when {
        weight != null && label != null -> "$source -[$label ($weight)]-> $target"
        weight == null || label == null -> "$source -[${weight ?: label}]-> $target"
        else -> "$source -> $target"
      }
}
