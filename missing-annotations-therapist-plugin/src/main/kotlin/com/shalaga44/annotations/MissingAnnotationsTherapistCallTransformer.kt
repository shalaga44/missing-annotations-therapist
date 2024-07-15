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

package com.shalaga44.annotations

import com.shalaga44.annotations.utils.SourceFile
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class MissingAnnotationsTherapistCallTransformer(
  private val sourceFile: SourceFile,
  private val context: IrPluginContext,
  private val messageCollector: MessageCollector,
  private val functions: Set<FqName>,
  private val annotations: Set<FqName>,
  private val packageTargets: Set<FqName>,
) : IrElementTransformerVoidWithContext() {
  private val irTypeSystemContext = IrTypeSystemContextImpl(context.irBuiltIns)


  /*
    override fun visitClassNew(declaration: IrClass): IrStatement {
      val currentPackageFqName = declaration.packageFqName?.asString() ?: ""
      if (packageTargets.any { currentPackageFqName.startsWith(it.asString()) }) {
        messageCollector.warn(declaration, "Going inside $currentPackageFqName")
        annotations.forEach { addAnnotationToClass(declaration, it) }
      }else {
        messageCollector.warn(declaration, "Skipping inside $currentPackageFqName")
      }
      return super.visitClassNew(declaration)
    }
  */

  override fun visitClassNew(declaration: IrClass): IrStatement {
    annotations.forEach { addAnnotationToClass(declaration, it) }
    return super.visitClassNew(declaration)
  }

  private fun addAnnotationToClass(irClass: IrClass, annotationFqName: FqName) {
    val classId = ClassId.topLevel(annotationFqName)
    val annotationClass = context.referenceClass(classId)
    if (annotationClass != null) {
      val annotationCtor = annotationClass.owner.primaryConstructor ?: return
      val annotation = createAnnotation(annotationCtor.symbol)
      irClass.annotations = irClass.annotations + annotation
      messageCollector.info(irClass, "Added annotation @$annotationFqName to ${irClass.name}")
    } else {
      messageCollector.warn(irClass, "Annotation class @$annotationFqName not found")
    }
  }

  private fun createAnnotation(constructorSymbol: IrConstructorSymbol): IrConstructorCall {
    val builder = DeclarationIrBuilder(context, constructorSymbol)
    return builder.irCallConstructor(constructorSymbol, emptyList())
  }


  private fun MessageCollector.info(expression: IrElement, message: String) {
    report(expression, CompilerMessageSeverity.INFO, message)
  }

  private fun MessageCollector.warn(expression: IrElement, message: String) {
    report(expression, CompilerMessageSeverity.WARNING, message)
  }

  private fun MessageCollector.report(expression: IrElement, severity: CompilerMessageSeverity, message: String) {
    report(severity, message, sourceFile.getCompilerMessageLocation(expression))
  }
}

