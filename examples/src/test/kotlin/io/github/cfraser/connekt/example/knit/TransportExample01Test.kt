// This file was automatically generated from README.md by Knit tool. Do not edit.
package io.github.cfraser.connekt.example.knit

import kotlinx.knit.test.captureOutput
import kotlinx.knit.test.verifyOutputLines
import org.junit.jupiter.api.Tag
import kotlin.test.Test

@Tag("integration")
class TransportExample01Test {
  @Test
  fun testTransportExample01() {
  captureOutput("TransportExample01") { io.github.cfraser.connekt.example.knit.transportExample01.main() }.verifyOutputLines(
      "Hello, world!"
  )
  }
}
