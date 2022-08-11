# graph-it

[![Test](https://github.com/c-fraser/graph-it/workflows/Test/badge.svg)](https://github.com/c-fraser/graph-it/actions)
[![Release](https://img.shields.io/github/v/release/c-fraser/graph-it?logo=github&sort=semver)](https://github.com/c-fraser/graph-it/releases)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.c-fraser/graph-it.svg)](https://search.maven.org/search?q=g:io.github.c-fraser%20AND%20a:graph-it*)
[![Apache License 2.0](https://img.shields.io/badge/License-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

`graph-it` is a Kotlin (multiplatform) library for interacting with interconnected objects through a
flexible graph data structure.

## Contents

<!--- TOC -->

* [Features](#features)
* [Usage](#usage)
* [Examples](#examples)
  * [Build an undirected graph](#build-an-undirected-graph)
  * [Build a directed acyclic graph](#build-a-directed-acyclic-graph)
  * [Build a weighted graph](#build-a-weighted-graph)
  * [Build a graph with generic edge attributes](#build-a-graph-with-generic-edge-attributes)
  * [Traverse a graph depth-first](#traverse-a-graph-depth-first)
  * [Traverse a graph breadth-first](#traverse-a-graph-breadth-first)
  * [Find the shortest path between vertices](#find-the-shortest-path-between-vertices)
  * [Get the strongly connected components](#get-the-strongly-connected-components)
  * [Visualize a graph with Graphviz](#visualize-a-graph-with-graphviz)
* [License](#license)
* [Acknowledgements](#acknowledgements)

<!--- END -->

## Features

* Flexible `Graph` interface which supports
  multiple [graph types](https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)#Definitions).
* Vertices and be `Any` type.
* Edges may contain a `weight` or generic `attributes`.
* Type-safe builder DSL which validates the graph type constraints.
* [Depth-first](https://en.wikipedia.org/wiki/Depth-first_search)
  and [breadth-first](https://en.wikipedia.org/wiki/Breadth-first_search) traversal.
* [Dijkstra's](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm) shortest path algorithm.
* [Tarjan's](https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm)
  strongly connected components algorithm.
* `Graph` visualization through [Graphviz](https://graphviz.org/).

## Usage

The `graph-it` library is accessible
via [Maven Central](https://search.maven.org/search?q=g:io.github.c-fraser%20AND%20a:graph-it*).

## Examples

### Build an undirected graph

![example-01](docs/example-01.svg)

<!--- TEST_NAME Example01Test --> 

<!--- INCLUDE
import io.github.cfraser.graphit.buildGraph

fun runExample01() {
println(
----- SUFFIX 
)
}
-->

```kotlin
buildGraph {
  this += 1 to 2
  this += 1 to 4
  this += 2 to 3
  this += 2 to 4
  this += 2 to 5
  this += 3 to 5
}
```

<!--- KNIT Example01.kt --> 
<!--- TEST
strict graph {
1 -- 2;
1 -- 4;
2 -- 3;
2 -- 4;
2 -- 5;
3 -- 5;
}
-->

<!---
echo '
strict graph {
1 -- 2;
1 -- 4;
2 -- 3;
2 -- 4;
2 -- 5;
3 -- 5;
}
' | dot -Tsvg > docs/example-01.svg
-->

### Build a directed acyclic graph

![example-02](docs/example-02.svg)

<!--- TEST_NAME Example02Test --> 

<!--- INCLUDE
import io.github.cfraser.graphit.Feature.ACYCLIC
import io.github.cfraser.graphit.Feature.DIRECTED
import io.github.cfraser.graphit.buildGraph

fun runExample02() {
println(
----- SUFFIX 
)
}
-->

```kotlin
buildGraph(DIRECTED, ACYCLIC) {
  this += 1 to 2
  this += 1 to 3
  this += 2 to 3
  this += 2 to 4
  this += 3 to 4
}
```

<!--- KNIT Example02.kt --> 
<!--- TEST
strict digraph {
1 -> 2;
1 -> 3;
2 -> 3;
2 -> 4;
3 -> 4;
}
-->

<!---
echo '
strict digraph {
1 -> 2;
1 -> 3;
2 -> 3;
2 -> 4;
3 -> 4;
}
' | dot -Tsvg > docs/example-02.svg
-->

### Build a weighted graph

![example-03](docs/example-03.svg)

<!--- TEST_NAME Example03Test --> 

<!--- INCLUDE
import io.github.cfraser.graphit.buildGraph

fun runExample03() {
println(
----- SUFFIX 
)
}
-->

```kotlin
buildGraph {
  this += "A" to "B" weighs 3
  this += "A" to "C" weighs 1
  this += "A" to "D" weighs 5
  this += "B" to "D" weighs 6
  this += "B" to "C" weighs 2
  this += "C" to "D" weighs 4
}
```

<!--- KNIT Example03.kt --> 
<!--- TEST
strict graph {
A -- B [weight=3, label=3];
A -- C [weight=1, label=1];
A -- D [weight=5, label=5];
B -- D [weight=6, label=6];
B -- C [weight=2, label=2];
C -- D [weight=4, label=4];
}
-->

<!---
echo '
strict graph {
A -- B [weight=3, label=3];
A -- C [weight=1, label=1];
A -- D [weight=5, label=5];
B -- D [weight=6, label=6];
B -- C [weight=2, label=2];
C -- D [weight=4, label=4];
}
' | dot -Tsvg > docs/example-03.svg
-->

### Build a graph with generic edge attributes

![example-04](docs/example-04.svg)

<!--- TEST_NAME Example04Test --> 

<!--- INCLUDE
import io.github.cfraser.graphit.buildGraph

fun runExample04() {
println(
----- SUFFIX 
)
}
-->

```kotlin
buildGraph {
  this += 1 to 2 with "A"
  this += 2 to 3 with "B"
  this += 1 to 4 with "C"
  this += 4 to 5 with "D"
}
```

<!--- KNIT Example04.kt --> 
<!--- TEST
strict graph {
1 -- 2 [label="A"];
1 -- 4 [label="C"];
2 -- 3 [label="B"];
4 -- 5 [label="D"];
}
-->

<!---
echo '
strict graph {
1 -- 2 [label="A"];
1 -- 4 [label="C"];
2 -- 3 [label="B"];
4 -- 5 [label="D"];
}
' | dot -Tsvg > docs/example-04.svg
-->

### Traverse a graph depth-first

![example-05](docs/example-05.svg)

<!--- TEST_NAME Example05Test --> 

<!--- INCLUDE
import io.github.cfraser.graphit.buildGraph
import io.github.cfraser.graphit.DepthFirst
import io.github.cfraser.graphit.Feature.DIRECTED

fun runExample05() {
----- SUFFIX
}
-->

```kotlin
val graph = buildGraph(DIRECTED) {
  this += 1 to 2
  this += 1 to 3
  this += 3 to 4
}
val vertices = graph.traverse(DepthFirst(1))
println(vertices.joinToString())
```

```text
1, 3, 4, 2
```

<!--- KNIT Example05.kt --> 
<!--- TEST -->

<!---
echo '
strict digraph {
1 -> 2;
1 -> 3;
3 -> 4;
}
' | dot -Tsvg > docs/example-05.svg
-->

### Traverse a graph breadth-first

![example-06](docs/example-06.svg)

<!--- TEST_NAME Example06Test --> 

<!--- INCLUDE
import io.github.cfraser.graphit.BreadthFirst
import io.github.cfraser.graphit.buildGraph
import io.github.cfraser.graphit.Feature.DIRECTED

fun runExample06() {
----- SUFFIX
}
-->

```kotlin
val graph = buildGraph(DIRECTED) {
  this += 1 to 2
  this += 1 to 3
  this += 3 to 4
}
val vertices = graph.traverse(BreadthFirst(1))
println(vertices.joinToString())
```

```text
1, 2, 3, 4
```

<!--- KNIT Example06.kt --> 
<!--- TEST -->

<!---
echo '
strict digraph {
1 -> 2;
1 -> 3;
3 -> 4;
}
' | dot -Tsvg > docs/example-06.svg
-->

### Find the shortest path between vertices

![example-07](docs/example-07.svg)

<!--- TEST_NAME Example07Test --> 

<!--- INCLUDE
import io.github.cfraser.graphit.buildGraph
import io.github.cfraser.graphit.DepthFirst

fun runExample07() {
----- SUFFIX
}
-->

```kotlin
val graph = buildGraph {
  this += "A" to "C" weighs 3
  this += "A" to "F" weighs 2
  this += "C" to "F" weighs 2
  this += "C" to "D" weighs 4
  this += "C" to "E" weighs 1
  this += "F" to "E" weighs 3
  this += "F" to "B" weighs 6
  this += "F" to "G" weighs 5
  this += "D" to "B" weighs 1
  this += "E" to "B" weighs 2
  this += "G" to "B" weighs 2
}
val path = graph.shortestPath("A" to "B")
println(path.joinToString())
```

```text
A, C, E, B
```

<!--- KNIT Example07.kt --> 
<!--- TEST -->

<!---
echo '
strict graph {
A -- C [weight=3, label=3];
A -- F [weight=2, label=2];
C -- F [weight=2, label=2];
C -- D [weight=4, label=4];
C -- E [weight=1, label=1];
F -- E [weight=3, label=3];
F -- B [weight=6, label=6];
F -- G [weight=5, label=5];
D -- B [weight=1, label=1];
E -- B [weight=2, label=2];
B -- G [weight=2, label=2];
}
' | dot -Tsvg > docs/example-07.svg
-->

### Get the strongly connected components

![example-08](docs/example-08.svg)

<!--- TEST_NAME Example08Test --> 

<!--- INCLUDE
import io.github.cfraser.graphit.buildGraph
import io.github.cfraser.graphit.Feature.DIRECTED

fun runExample08() {
----- SUFFIX
}
-->

```kotlin
val graph = buildGraph(DIRECTED) {
  this += 1 to 2
  this += 2 to 3
  this += 2 to 5
  this += 2 to 6
  this += 3 to 4
  this += 3 to 7
  this += 4 to 3
  this += 4 to 8
  this += 5 to 1
  this += 5 to 6
  this += 6 to 7
  this += 7 to 6
  this += 8 to 4
  this += 8 to 7
}
val components = graph.stronglyConnectedComponents()
println(components)
```

```text
[[6, 7], [8, 4, 3], [5, 2, 1]]
```

<!--- KNIT Example08.kt --> 
<!--- TEST -->

<!---
echo '
strict digraph {
1 -> 2;
2 -> 3;
2 -> 5;
2 -> 6;
3 -> 4;
3 -> 7;
5 -> 1;
5 -> 6;
6 -> 7;
4 -> 3;
4 -> 8;
7 -> 6;
8 -> 4;
8 -> 7;
}
' | dot -Tsvg > docs/example-08.svg
-->

### Visualize a graph with Graphviz

`Graph.toString()` returns the [DOT language](https://graphviz.org/doc/info/lang.html)
representation of the graph instance.

```kotlin
val graph = buildGraph {
  this += 1 to 2
  this += 1 to 3
}
// Print the graph description
println("$graph")
// Or... write the graph description to a file
File("graph.dot").writeText("$graph")
```

A visualization of the graph can be generated from the graph description using
the [dot command](https://graphviz.org/doc/info/command.html).

```bash
# Pipe in the graph description
echo "$GRAPH_DESCRIPTION" | dot -Tsvg > graph.dot.svg
# Or... specify the graph description file
dot -Tsvg -O graph.dot
```

## License

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

## Acknowledgements

Kudos to the [(Go) graph package](https://github.com/dominikbraun/graph) which significantly
influenced the implementation of `graph-it`.
