@file:OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)

package io.github.cfraser.graphit

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

/**
 * Build a [Graph] instance.
 *
 * The characteristics of the [Graph] are customizable through the [features].
 *
 * The [builder] function is used to populate the [Graph] with vertices and edges.
 *
 * @param V the type of each vertex in the graph
 * @param E the type of each edge label in the graph
 * @param features the graph features
 * @param builder the function adding vertices and edges to the [Graph]
 * @return the graph
 */
fun <V : Any, E : Any> buildGraph(
    vararg features: Feature = arrayOf(),
    @BuilderInference builder: MutableGraph<V, E>.() -> Unit
): Graph<V, E> {
  contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
  return TODO()
}
