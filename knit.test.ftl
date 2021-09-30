// This file was automatically generated from ${file.name} by Knit tool. Do not edit.
package ${test.package}

import kotlinx.knit.test.captureOutput
import kotlinx.knit.test.verifyOutputLines
import org.junit.jupiter.api.Tag
import kotlin.test.Test

@Tag("integration")
class ${test.name} {
<#list cases as case><#assign method = test["mode.${case.param}"]!"custom">
  @Test
  fun test${case.name}() {
  captureOutput("${case.name}") { ${case.knit.package}.${case.knit.name}.main() }<#if method != "custom">.${method}(
    <#list case.lines as line>
      "${line?j_string}"<#sep>,</#sep>
    </#list>
  )
<#else>.also { lines ->
  check(${case.param})
  }
</#if>
  }
    <#sep>

</#list>
}