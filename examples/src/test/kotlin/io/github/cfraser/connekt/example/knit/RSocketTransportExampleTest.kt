// This file was automatically generated from README.md by Knit tool. Do not edit.
package io.github.cfraser.connekt.example.knit

import kotlinx.knit.test.captureOutput
import kotlinx.knit.test.verifyOutputLines
import org.junit.jupiter.api.Tag
import kotlin.test.Test

@Tag("integration")
class RSocketTransportExampleTest {
  @Test
  fun testRsocketTransportExample01() {
  captureOutput("RsocketTransportExample01") { io.github.cfraser.connekt.example.knit.rsocketTransportExample01.main() }.verifyOutputLines(
      "Hello, world!"
  )
  }
}
