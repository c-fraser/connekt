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
