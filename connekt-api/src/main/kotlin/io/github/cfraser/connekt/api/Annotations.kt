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
package io.github.cfraser.connekt.api

/** The [InternalConnektApi] annotation is applied to **internal** *connekt* APIs. */
@Retention(value = AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal 'connekt' API that should not be used externally.")
annotation class InternalConnektApi

/**
 * The [ExperimentalTransport] annotation is applied to [Transport] implementations that are
 * designated as *experimental*.
 */
@Suppress("unused")
@Retention(value = AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING, message = "This is an experimental 'connekt' transport.")
annotation class ExperimentalTransport
