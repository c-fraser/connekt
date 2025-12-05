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
package io.github.cfraser.connekt

/** [isDirected] returns `true` if [Feature.DIRECTED] is in the [Graph.features]. */
val Graph<*, *>.isDirected: Boolean
  get() = Feature.DIRECTED in features

/** [isUndirected] returns `true` if [Feature.DIRECTED] is **not** in the [Graph.features]. */
val Graph<*, *>.isUndirected: Boolean
  get() = !isDirected

/** [isAcyclic] returns `true` if [Feature.ACYCLIC] is in the [Graph.features]. */
val Graph<*, *>.isAcyclic: Boolean
  get() = Feature.ACYCLIC in features

/**
 * [Feature] describes the qualities supported and enforced by a [Graph]. A [Graph] may have any
 * combination of feature(s) or none.
 */
enum class Feature {

  /**
   * Specifies the edges in the graph are [directed](https://en.wikipedia.org/wiki/Directed_graph).
   */
  DIRECTED,

  /**
   * Specifies the edges in the graph are [acyclic](https://en.wikipedia.org/wiki/Acyclic_graph).
   */
  ACYCLIC,
}
