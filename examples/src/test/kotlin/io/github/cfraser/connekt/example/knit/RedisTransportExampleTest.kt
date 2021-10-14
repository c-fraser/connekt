// This file was automatically generated from README.md by Knit tool. Do not edit.
package io.github.cfraser.connekt.example.knit

import kotlinx.knit.test.captureOutput
import kotlinx.knit.test.verifyOutputLines
import org.junit.jupiter.api.Tag
import kotlin.test.Test

@Tag("integration")
class RedisTransportExampleTest {
  @Test
  fun testRedisTransportExample01() {
  captureOutput("RedisTransportExample01") { io.github.cfraser.connekt.example.knit.redisTransportExample01.main() }.verifyOutputLines(
      "Hello, world!"
  )
  }
}
