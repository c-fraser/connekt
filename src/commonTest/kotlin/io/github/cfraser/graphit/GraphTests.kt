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

import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class GraphTests :
    StringSpec({
      "verify an undirected graph with basic edges" { graph().check(GraphType.UNDIRECTED) }
      "verify an undirected graph with weighted edges" {
        weightedGraph().check(GraphType.UNDIRECTED)
      }
      "verify an undirected graph with generic edges" { genericGraph().check(GraphType.UNDIRECTED) }
      "verify an undirected graph with weighted generic edges" {
        weightedGenericGraph().check(GraphType.UNDIRECTED)
      }
      "verify a directed graph with basic edges" {
        graph(Feature.DIRECTED).check(GraphType.DIRECTED)
      }
      "verify a directed graph with weighted edges" {
        weightedGraph(Feature.DIRECTED).check(GraphType.DIRECTED)
      }
      "verify a directed graph with generic edges" {
        genericGraph(Feature.DIRECTED).check(GraphType.DIRECTED)
      }
      "verify a directed graph with weighted generic edges" {
        weightedGenericGraph(Feature.DIRECTED).check(GraphType.DIRECTED)
      }
      "verify an acyclic graph with basic edges" { graph(Feature.ACYCLIC).check(GraphType.ACYCLIC) }
      "verify an acyclic graph with weighted edges" {
        weightedGraph(Feature.ACYCLIC).check(GraphType.ACYCLIC)
      }
      "verify an acyclic graph with generic edges" {
        genericGraph(Feature.ACYCLIC).check(GraphType.ACYCLIC)
      }
      "verify an acyclic graph with weighted generic edges" {
        weightedGenericGraph(Feature.ACYCLIC).check(GraphType.ACYCLIC)
      }
      "verify a directed acyclic graph with basic edges" {
        graph(Feature.DIRECTED, Feature.ACYCLIC).check(GraphType.DIRECTED_ACYCLIC)
      }
      "verify a directed acyclic graph with weighted edges" {
        weightedGraph(Feature.DIRECTED, Feature.ACYCLIC).check(GraphType.DIRECTED_ACYCLIC)
      }
      "verify a directed acyclic graph with generic edges" {
        genericGraph(Feature.DIRECTED, Feature.ACYCLIC).check(GraphType.DIRECTED_ACYCLIC)
      }
      "verify a directed acyclic graph with weighted generic edges" {
        weightedGenericGraph(Feature.DIRECTED, Feature.ACYCLIC).check(GraphType.DIRECTED_ACYCLIC)
      }
      "verify cycle check for a directed acyclic graph" {
        buildGraph(Feature.DIRECTED, Feature.ACYCLIC) {
          this += "a" to "b"
          this += "b" to "c"
          shouldThrow<AcyclicException> {
            this += "c" to "a"
            fail("Expected acyclic exception")
          }
        }
      }
      "TEST" {
        println(
            buildGraph {
              this += 1 to 2 with "A"
              this += 2 to 3 with "B"
              this += 1 to 4 with "C"
              this += 4 to 5 with "D"
            })
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
      get() = fail("Unknown edge type")

    fun graph(vararg features: Feature) =
        buildGraph(*features) {
          this += "a" to "b"
          checkEdgeAlreadyExists { this += "a" to "c" }
          checkLoop { this += "a" to "a" }
          if (Feature.ACYCLIC in features && Feature.DIRECTED !in features)
              checkAcyclic { this += "b" to "c" }
        }

    fun weightedGraph(vararg features: Feature) =
        buildGraph(*features) {
          this += "a" to "b" weighs 1
          checkEdgeAlreadyExists { this += "a" to "c" weighs 2 }
          checkLoop { this += "a" to "a" weighs 0 }
          if (Feature.ACYCLIC in features && Feature.DIRECTED !in features)
              checkAcyclic { this += "b" to "c" weighs 0 }
        }

    fun genericGraph(vararg features: Feature) =
        buildGraph(*features) {
          this += "a" to "b" with 1.0
          checkEdgeAlreadyExists { this += "a" to "c" with 2.0 }
          checkLoop { this += "a" to "a" with 0.0 }
          if (Feature.ACYCLIC in features && Feature.DIRECTED !in features)
              checkAcyclic { this += "b" to "c" with 0.0 }
        }

    fun weightedGenericGraph(vararg features: Feature) =
        buildGraph(*features) {
          this += "a" to "b" weighs 1 with 1.0
          checkEdgeAlreadyExists { this += "a" to "c" weighs 2 with 2.0 }
          checkLoop { this += "a" to "a" weighs 0 with 0.0 }
          if (Feature.ACYCLIC in features && Feature.DIRECTED !in features)
              checkAcyclic { this += "b" to "c" weighs 0 with 0.0 }
        }

    fun <E : Edge<String>> GraphBuilder<String, E>.checkEdgeAlreadyExists(
        block: GraphBuilder<String, E>.() -> Unit
    ) = repeat(2) { if (it == 0) block() else shouldThrow<EdgeAlreadyExists> { block() } }

    fun <E : Edge<String>> GraphBuilder<String, E>.checkLoop(
        block: GraphBuilder<String, E>.() -> Unit
    ) = shouldThrow<LoopException> { block() }

    fun <E : Edge<String>> GraphBuilder<String, E>.checkAcyclic(
        block: GraphBuilder<String, E>.() -> Unit
    ) = shouldThrow<AcyclicException> { block() }

    enum class GraphType {
      UNDIRECTED,
      DIRECTED,
      ACYCLIC,
      DIRECTED_ACYCLIC
    }

    inline fun <reified E : Edge<String>> Graph<String, E>.check(type: GraphType) {
      when (type) {
        GraphType.UNDIRECTED -> {
          features shouldBe emptySet()
          isUndirected shouldBe true
          isAcyclic shouldBe false
        }
        GraphType.DIRECTED -> {
          features shouldContainExactly setOf(Feature.DIRECTED)
          isDirected shouldBe true
          isAcyclic shouldBe false
        }
        GraphType.ACYCLIC -> {
          features shouldContainExactly setOf(Feature.ACYCLIC)
          isUndirected shouldBe true
          isAcyclic shouldBe true
        }
        GraphType.DIRECTED_ACYCLIC -> {
          features shouldContainExactlyInAnyOrder setOf(Feature.DIRECTED, Feature.ACYCLIC)
          isDirected shouldBe true
          isAcyclic shouldBe true
        }
      }
      checkContainsVertex()
      checkContainsEdge()
      checkGetEdges()
      checkGetEdge()
      checkTraverse()
      checkShortestPath()
      checkToString()
    }

    fun Graph<String, *>.checkContainsVertex() {
      VERTICES.all { it in this } shouldBe true
      INVALID_VERTICES.all { it !in this } shouldBe true
    }

    fun Graph<String, *>.checkContainsEdge() {
      EDGES.all { it in this } shouldBe true
      (("b" to "c") !in this) shouldBe true
      shouldThrow<VertexNotFound> { ("x" to "y") in this }
    }

    inline fun <reified E : Edge<String>> Graph<String, E>.checkGetEdges() {
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

    inline fun <reified E : Edge<String>> Graph<String, E>.checkGetEdge() {
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

    fun Graph<String, *>.checkTraverse() {
      traverse(DepthFirst("a")).toSet() shouldContainExactlyInAnyOrder VERTICES
      traverse(BreadthFirst("a")).toSet() shouldContainExactlyInAnyOrder VERTICES
      if (isDirected) {
        traverse(DepthFirst("b")) shouldContainExactly listOf("b")
        traverse(BreadthFirst("c")) shouldContainExactly listOf("c")
      }
    }

    fun Graph<String, *>.checkShortestPath() {
      if (isDirected) shortestPath("a" to "b") shouldContainExactly listOf("a", "b")
      else shortestPath("b" to "c") shouldContainExactly listOf("b", "a", "c")
      if (isDirected) shouldThrow<NoPathExists> { shortestPath("b" to "c") }
    }

    inline fun <reified E : Edge<String>> Graph<String, E>.checkToString() {
      val statement = { source: String, target: String, i: Int ->
        "$source ${if (isDirected) "->" else "--"} $target${when (E::class) {
          BasicEdge::class -> null
          WeightedEdge::class -> "[weight=$i, label=$i]"
          GenericEdge::class -> "[label=\"${i.toDouble()}\"]"
          WeightedGenericEdge::class -> "[weight=$i, label=\"weight: $i, attributes: ${i.toDouble()}\"]"
          else -> UNKNOWN_EDGE_TYPE
        }?.let { " $it" }.orEmpty() + ";"}"
      }
      "$this" shouldBe
          """|strict ${if (isDirected) "digraph" else "graph"} {
          |${statement("a", "b", 1)}
          |${statement("a", "c", 2)}
          |}""".trimMargin()
    }
  }
}
