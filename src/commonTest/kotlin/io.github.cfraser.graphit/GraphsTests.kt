@file:Suppress("unused")

package io.github.cfraser.graphit

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class GraphsTests :
    StringSpec({
      "verify a graph with basic edges" {
        val graph = buildGraph {
          this += "a" to "b"
          this += "a" to "c"
        }

        graph.checkVertices()
        graph["a"] shouldContainExactlyInAnyOrder setOf(BasicEdge("a", "b"), BasicEdge("a", "c"))
      }

      "verify a graph with weighted edges" {
        val graph = buildGraph {
          this += "a" to "b" weighs 1
          this += "a" to "c" weighs 2
        }

        graph.checkVertices()
        graph["a"] shouldContainExactlyInAnyOrder
            setOf(WeightedEdge("a", "b", 1), WeightedEdge("a", "c", 2))
      }

      "verify a graph with generic edges" {
        val graph = buildGraph {
          this += "a" to "b" with 1.0
          this += "a" to "c" with 2.0
        }

        graph.checkVertices()
        graph["a"] shouldContainExactlyInAnyOrder
            setOf(GenericEdge("a", "b", 1.0), GenericEdge("a", "c", 2.0))
      }

      "verify a graph with weighted generic edges" {
        val graph = buildGraph {
          this += "a" to "b" weighs 1 with 1.0
          this += "a" to "c" weighs 2 with 2.0
        }

        graph.checkVertices()
        graph["a"] shouldContainExactlyInAnyOrder
            setOf(WeightedGenericEdge("a", "b", 1, 1.0), WeightedGenericEdge("a", "c", 2, 2.0))
      }
    }) {

  private companion object {

    inline fun Graph<String, *>.checkVertices(vertices: Set<String> = setOf("a", "b", "c")) =
        traverse().toSet() shouldContainExactlyInAnyOrder vertices
  }
}
