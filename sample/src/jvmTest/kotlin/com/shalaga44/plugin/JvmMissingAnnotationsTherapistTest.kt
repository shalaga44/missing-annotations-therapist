
package com.shalaga44.plugin

import com.project.common.dto.Hello
import com.project.common.main.HelloWithDto
//import com.project.common.dto.HelloWithDto
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test

class JvmMissingAnnotationsTherapistTest {
  @Test
  fun assert() {
    val hello = Hello("Therapist?")
    val helloWithDto = HelloWithDto("Therapist?")
    val annotations = hello::class.annotations
    assertTrue(annotations.isNotEmpty())
//    assertSame(annotations, listOf(MyDto::class.simpleName))
  }
}
