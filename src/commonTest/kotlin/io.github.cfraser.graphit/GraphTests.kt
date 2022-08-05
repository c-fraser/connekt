@file:Suppress("unused")

package io.github.cfraser.graphit

import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class GraphTests :
    StringSpec({
      "verify an undirected graph with basic edges" { abcGraph().checkUndirected().checkAbcGraph() }
      "verify an undirected graph with weighted edges" {
        weightedAbcGraph().checkUndirected().checkAbcGraph()
      }
      "verify an undirected graph with generic edges" {
        genericAbcGraph().checkUndirected().checkAbcGraph()
      }
      "verify an undirected graph with weighted generic edges" {
        weightedGenericAbcGraph().checkUndirected().checkAbcGraph()
      }
      "verify a directed graph with basic edges" {
        abcGraph(Feature.DIRECTED).checkDirected().checkAbcGraph()
      }
      "verify a directed graph with weighted edges" {
        weightedAbcGraph(Feature.DIRECTED).checkDirected().checkAbcGraph()
      }
      "verify a directed graph with generic edges" {
        genericAbcGraph(Feature.DIRECTED).checkDirected().checkAbcGraph()
      }
      "verify a directed graph with weighted generic edges" {
        weightedGenericAbcGraph(Feature.DIRECTED).checkDirected().checkAbcGraph()
      }
    }) {

  private companion object {

    val VERTICES = setOf("a", "b", "c")
    val INVALID_VERTICES = setOf("x", "y", "z")
    val EDGES = setOf("a" to "b", "a" to "c")
    val A_B_BASIC_EDGE = BasicEdge("a", "b")
    val A_C_BASIC_EDGE = BasicEdge("a", "c")
    val A_B_WEIGHTED_EDGE = WeightedEdge("a", "b", 1)
    val A_C_WEIGHTED_EDGE = WeightedEdge("a", "c", 2)
    val A_B_GENERIC_EDGE = GenericEdge("a", "b", 1.0)
    val A_C_GENERIC_EDGE = GenericEdge("a", "c", 2.0)
    val A_B_WEIGHTED_GENERIC_EDGE = WeightedGenericEdge("a", "b", 1, 1.0)
    val A_C_WEIGHTED_GENERIC_EDGE = WeightedGenericEdge("a", "c", 2, 2.0)

    val UNKNOWN_EDGE_TYPE: Nothing
      get() = fail("An io.github.cfraser.graphit.Edge is required")

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

    fun <E : Edge<String>> Graph<String, E>.checkUndirected() = apply {
      features shouldBe emptySet()
    }

    fun <E : Edge<String>> Graph<String, E>.checkDirected() = apply {
      features shouldContainExactly setOf(Feature.DIRECTED)
    }

    fun <E : Edge<String>> Graph<String, E>.checkAcyclic() = apply {
      features shouldContainExactly setOf(Feature.ACYCLIC)
    }

    fun <E : Edge<String>> Graph<String, E>.checkDirectedAcyclic() = apply {
      features shouldContainExactlyInAnyOrder setOf(Feature.DIRECTED, Feature.ACYCLIC)
    }

    inline fun <reified E : Edge<String>> Graph<String, E>.checkAbcGraph() {
      checkAbcGraphContainsVertex()
      checkAbcGraphContainsEdge()
      checkAbcGraphGetEdges()
      checkAbcGraphGetEdge()
      checkAbcGraphTraverse()
    }

    fun Graph<String, *>.checkAbcGraphContainsVertex() {
      VERTICES.all { it in this } shouldBe true
      INVALID_VERTICES.all { it !in this } shouldBe true
    }

    fun Graph<String, *>.checkAbcGraphContainsEdge() {
      EDGES.all { it in this } shouldBe true
      (("b" to "c") !in this) shouldBe true
      shouldThrow<VertexNotFound> { ("x" to "y") in this }
    }

    inline fun <reified E : Edge<String>> Graph<String, E>.checkAbcGraphGetEdges() {
      this["a"] shouldContainExactlyInAnyOrder
          when (E::class) {
            BasicEdge::class -> setOf(A_B_BASIC_EDGE, A_C_BASIC_EDGE)
            WeightedEdge::class -> setOf(A_B_WEIGHTED_EDGE, A_C_WEIGHTED_EDGE)
            GenericEdge::class -> setOf(A_B_GENERIC_EDGE, A_C_GENERIC_EDGE)
            WeightedGenericEdge::class ->
                setOf(A_B_WEIGHTED_GENERIC_EDGE, A_C_WEIGHTED_GENERIC_EDGE)
            else -> UNKNOWN_EDGE_TYPE
          }
      this["b"] shouldContainExactlyInAnyOrder
          when (E::class) {
            BasicEdge::class -> setOf(A_B_BASIC_EDGE)
            WeightedEdge::class -> setOf(A_B_WEIGHTED_EDGE)
            GenericEdge::class -> setOf(A_B_GENERIC_EDGE)
            WeightedGenericEdge::class -> setOf(A_B_WEIGHTED_GENERIC_EDGE)
            else -> UNKNOWN_EDGE_TYPE
          }
      this["c"] shouldContainExactlyInAnyOrder
          when (E::class) {
            BasicEdge::class -> setOf(A_C_BASIC_EDGE)
            WeightedEdge::class -> setOf(A_C_WEIGHTED_EDGE)
            GenericEdge::class -> setOf(A_C_GENERIC_EDGE)
            WeightedGenericEdge::class -> setOf(A_C_WEIGHTED_GENERIC_EDGE)
            else -> UNKNOWN_EDGE_TYPE
          }
      shouldThrow<VertexNotFound> { this["x"] }
    }

    inline fun <reified E : Edge<String>> Graph<String, E>.checkAbcGraphGetEdge() {
      this["a" to "b"] shouldBe
          when (E::class) {
            BasicEdge::class -> A_B_BASIC_EDGE
            WeightedEdge::class -> A_B_WEIGHTED_EDGE
            GenericEdge::class -> A_B_GENERIC_EDGE
            WeightedGenericEdge::class -> A_B_WEIGHTED_GENERIC_EDGE
            else -> UNKNOWN_EDGE_TYPE
          }
      this["a" to "c"] shouldBe
          when (E::class) {
            BasicEdge::class -> A_C_BASIC_EDGE
            WeightedEdge::class -> A_C_WEIGHTED_EDGE
            GenericEdge::class -> A_C_GENERIC_EDGE
            WeightedGenericEdge::class -> A_C_WEIGHTED_GENERIC_EDGE
            else -> UNKNOWN_EDGE_TYPE
          }
      shouldThrow<EdgeNotFound> { this["b" to "c"] }
      shouldThrow<VertexNotFound> { this["x" to "y"] }
    }

    fun Graph<String, *>.checkAbcGraphTraverse() {
      traverse(DepthFirst("a")).toSet() shouldContainExactlyInAnyOrder VERTICES
      if (Feature.DIRECTED in features) {}
    }
  }
}
