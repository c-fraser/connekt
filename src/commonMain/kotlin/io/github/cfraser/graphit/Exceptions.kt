package io.github.cfraser.graphit

/**
 * [GraphException] is the base [RuntimeException] type representing erroneous [Graph] operations.
 */
sealed class GraphException(override val message: String?) : RuntimeException()

/** [VertexNotFound] is thrown when an expected vertex is not found in the graph. */
class VertexNotFound(value: Any) : GraphException("Vertex $value not found")

/** [EdgeAlreadyExists] is thrown when an edge between the vertices already exists in the graph. */
class EdgeAlreadyExists(source: Any, target: Any) :
    GraphException("Edge from $source to $target already exists")

/** [EdgeNotFound] is thrown when an edge between the vertices is not found in the graph. */
class EdgeNotFound(source: Any, target: Any) :
    GraphException("Edge from $source to $target not found")

/**
 * [WeightedException] is thrown when a weighted graph attempts to create an edge without a weight.
 */
object WeightedException : GraphException("A weighted graph cannot contain a unweighted edge")

/**
 * [UnweightedException] is thrown when an unweighted graph attempts to create or access a weighted
 * edge.
 */
object UnweightedException : GraphException("An unweighted graph cannot contain a weighted edge")

/** [UndirectedException] is thrown when an undirected graph attempts to find strong connections. */
object UndirectedException :
    GraphException("An undirected graph does not contain strong connections")

/** [NoPathExists] is thrown when no path between the vertices exists in the graph. */
class NoPathExists(source: Any, target: Any) :
    GraphException("No path from $source to $target exists")

/** [LabeledException] is thrown when a labeled graph attempts to create an edge without a label. */
object LabeledException : GraphException("A labeled graph cannot contain an unlabeled edge")

/**
 * [UnlabeledException] is thrown when an unlabeled graph attempts to create or access a labeled
 * edge.
 */
object UnlabeledException : GraphException("An unlabeled graph cannot contain a labeled edge")
