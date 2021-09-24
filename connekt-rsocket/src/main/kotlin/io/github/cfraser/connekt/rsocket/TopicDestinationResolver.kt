/*
Copyright 2021 c-fraser

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
package io.github.cfraser.connekt.rsocket

import java.net.InetSocketAddress

/**
 * The [TopicDestinationResolver] type is responsible for determining the IP socket address(es) for
 * a *topic*.
 */
interface TopicDestinationResolver {

  /**
   * Return the resolved the IP socket address(es).
   *
   * @param topic the *topic* to get the IP socket address(es) for
   * @return the [InetSocketAddress] instances for the *topic*
   */
  suspend fun resolve(topic: String): Set<InetSocketAddress>
}
