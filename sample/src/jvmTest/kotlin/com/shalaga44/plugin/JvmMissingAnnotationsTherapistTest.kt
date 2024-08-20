/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shalaga44.plugin

import com.project.common.dto.Hello
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test

class JvmMissingAnnotationsTherapistTest {
  @Test
  fun assert() {
    val hello = Hello("Therapist?")
    val annotations = hello::class.annotations.map { it::class }
    assertTrue(annotations.isNotEmpty())
//    assertSame(annotations, listOf(MyDto::class.simpleName))
  }
}
