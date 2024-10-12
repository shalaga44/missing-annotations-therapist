package com.shalaga44.annotations

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import java.io.OutputStream
import java.lang.reflect.InvocationTargetException
import kotlin.test.assertEquals

private val DEFAULT_COMPILER_PLUGIN_REGISTRARS = arrayOf(
  FirMissingAnnotationsTherapistCompilerPluginRegistrar(),
)


fun compile(
  sources: List<SourceFile>,
  vararg compilerPluginRegistrars: CompilerPluginRegistrar = DEFAULT_COMPILER_PLUGIN_REGISTRARS,
): JvmCompilationResult {
  return KotlinCompilation().apply {
    this.sources = sources
    messageOutputStream = object : OutputStream() {
      override fun write(b: Int) {
        // Discard all output
      }

      override fun write(b: ByteArray, off: Int, len: Int) {
        // Discard all output
      }
    }
    this.compilerPluginRegistrars = compilerPluginRegistrars.toList()
    inheritClassPath = true
  }.compile()
}


fun execute(
  sources: List<SourceFile>,
  mainApplication: String = "MainKt",
  vararg compilerPluginRegistrars: CompilerPluginRegistrar = DEFAULT_COMPILER_PLUGIN_REGISTRARS,
) {
  val result = compile(
    sources,
    *compilerPluginRegistrars,
  )
  assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

  val kClazz = result.classLoader.loadClass(mainApplication)
  val main = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
  try {
    main.invoke(null)
  } catch (t: InvocationTargetException) {
    throw t.cause!!
  }
}


fun run(
  @Language("kotlin") mainSource: String,
  additionalSources: List<SourceFile> = emptyList(),
  mainApplication: String = "MainKt",
  vararg compilerPluginRegistrars: CompilerPluginRegistrar = DEFAULT_COMPILER_PLUGIN_REGISTRARS,
) = execute(
  sources = listOf(SourceFile.kotlin("main.kt", mainSource, trimIndent = false)) + additionalSources,
  mainApplication = mainApplication,
  *compilerPluginRegistrars,
)
