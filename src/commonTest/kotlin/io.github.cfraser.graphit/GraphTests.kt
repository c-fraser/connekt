@file:Suppress("unused")

package io.github.cfraser.graphit

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class GraphTests :
    StringSpec({
      "verify an undirected graph with basic edges" {
        val graph = abcGraph()
        graph.checkVertices()
        graph["a"] shouldContainExactlyInAnyOrder setOf(BasicEdge("a", "b"), BasicEdge("a", "c"))
      }

      "verify an undirected graph with weighted edges" {
        val graph = weightedAbcGraph()
        graph.checkVertices()
        graph["a"] shouldContainExactlyInAnyOrder
            setOf(WeightedEdge("a", "b", 1), WeightedEdge("a", "c", 2))
      }

      "verify an undirected graph with generic edges" {
        val graph = genericAbcGraph()
        graph.checkVertices()
        graph["a"] shouldContainExactlyInAnyOrder
            setOf(GenericEdge("a", "b", 1.0), GenericEdge("a", "c", 2.0))
      }

      "verify an undirected graph with weighted generic edges" {
        val graph = weightedGenericAbcGraph()
        graph.checkVertices()
        graph["a"] shouldContainExactlyInAnyOrder
            setOf(WeightedGenericEdge("a", "b", 1, 1.0), WeightedGenericEdge("a", "c", 2, 2.0))
      }

      "verify a directed graph with basic edges" {
        val graph = abcGraph(Feature.DIRECTED)
        graph.checkVertices()
        graph["a"] shouldContainExactlyInAnyOrder setOf(BasicEdge("a", "b"), BasicEdge("a", "c"))
      }

      "verify a directed graph with weighted edges" {
        val graph = weightedAbcGraph(Feature.DIRECTED)
        graph.checkVertices()
        graph["a"] shouldContainExactlyInAnyOrder
            setOf(WeightedEdge("a", "b", 1), WeightedEdge("a", "c", 2))
      }

      "verify a directed graph with generic edges" {
        val graph = genericAbcGraph(Feature.DIRECTED)
        graph.checkVertices()
        graph["a"] shouldContainExactlyInAnyOrder
            setOf(GenericEdge("a", "b", 1.0), GenericEdge("a", "c", 2.0))
      }

      "verify a directed graph with weighted generic edges" {
        val graph = weightedGenericAbcGraph(Feature.DIRECTED)
        graph.checkVertices()
        graph["a"] shouldContainExactlyInAnyOrder
            setOf(WeightedGenericEdge("a", "b", 1, 1.0), WeightedGenericEdge("a", "c", 2, 2.0))
      }
    }) {

  private companion object {

    fun abcGraph(vararg features: Feature) =
        buildGraph(*features) {
          this += "a" to "b"
          this += "a" to "c"
        }

    fun weightedAbcGraph(vararg features: Feature) =
        buildGraph(*features) {
          this += "a" to "b" weighs 1
          this += "a" to "c" weighs 2
        }

    fun genericAbcGraph(vararg features: Feature) =
        buildGraph(*features) {
          this += "a" to "b" with 1.0
          this += "a" to "c" with 2.0
        }

    fun weightedGenericAbcGraph(vararg features: Feature) =
        buildGraph(*features) {
          this += "a" to "b" weighs 1 with 1.0
          this += "a" to "c" weighs 2 with 2.0
        }

    fun Graph<String, *>.checkVertices(vertices: Set<String> = setOf("a", "b", "c")) =
        traverse(DepthFirst(vertices.random())).toSet() shouldContainExactlyInAnyOrder vertices
  }
}
