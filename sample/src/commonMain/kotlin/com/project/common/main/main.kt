package com.project.common.main

import com.project.common.dto.Hello
import com.project.common.dto.MyDto

@MyDto
class HelloWithDto(val value:String)


fun main() {
  val hello = Hello("Therapist")
  println(hello::class)
}
