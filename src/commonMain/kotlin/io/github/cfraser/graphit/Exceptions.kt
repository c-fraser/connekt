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
 * [GraphException] is the base [RuntimeException] type representing erroneous [Graph] operations.
 */
sealed class GraphException(override val message: String?) : RuntimeException()

/** [VertexNotFound] is thrown when an expected vertex is not found in the graph. */
class VertexNotFound(vertex: Any) : GraphException("Vertex $vertex not found")

/** [EdgeAlreadyExists] is thrown when an edge between the vertices already exists in the graph. */
class EdgeAlreadyExists(edge: Edge<*>) :
    GraphException("Edge from ${edge.source} to ${edge.target} already exists")

/** [EdgeNotFound] is thrown when an edge between the vertices is not found in the graph. */
class EdgeNotFound(source: Any, target: Any) :
    GraphException("Edge from $source to $target not found")

/** [NoPathExists] is thrown when no path between the vertices exists in the graph. */
class NoPathExists(source: Any, target: Any) :
    GraphException("No path from $source to $target exists")

/** [LoopException] is thrown when an edge connects a vertex to itself. */
object LoopException : GraphException("An edge in cannot connect a vertex to itself")

/**
 * [AcyclicException] is thrown when a path between the vertices would violate the acyclic
 * constraint in the graph.
 */
class AcyclicException(edge: Edge<*>) :
    GraphException("A path from ${edge.source} to ${edge.target} introduces a cycle in the graph")
