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
