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

package dev.shalaga44.mat

import com.google.auto.service.AutoService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.shalaga44.mat.CustomFirErrors.COMPILER_PLUGIN_INFO
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.InternalDiagnosticFactoryMethod
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.contains
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirAnnotationArgumentMappingImpl
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.visibilityForApproximation
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtElement

@AutoService(CompilerPluginRegistrar::class)
class MissingAnnotationsTherapistCompilerPluginRegistrar(
  private val args: MissingAnnotationsTherapistArgs? = null,
) : CompilerPluginRegistrar() {

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val configJson = configuration[KEY_CONFIG] ?: "{}"
    val gson = Gson()
    val type = object : TypeToken<MissingAnnotationsTherapist>() {}.type
    val gradleExtension = gson.fromJson<MissingAnnotationsTherapist?>(configJson, type)
    val annotations = gradleExtension?.annotations?.ifEmpty { null } ?: args?.annotations ?: return

    val pluginArgs = args ?: MissingAnnotationsTherapistArgs(
      annotations = annotations,
      enableLogging = gradleExtension.enableLogging,
    )

    val enableLogging = pluginArgs.enableLogging
    if (enableLogging) {
      System.err.println("FirMissingAnnotationsTherapistCompilerPluginRegistrar: Starting registration")
      System.err.println("Configuration JSON: $configJson")
      System.err.println("Logging is enabled")
    }

    if (enableLogging) {
      System.err.println("Parsed annotations: $gradleExtension")
      System.err.println("Parsed pluginArgs: $pluginArgs")
    }

    FirExtensionRegistrarAdapter.registerExtension(
      FirMissingAnnotationsTherapistExtensionRegistrar(pluginArgs),
    )

    if (enableLogging) {
      System.err.println("FirMissingAnnotationsTherapistCompilerPluginRegistrar: Registration completed")
    }
  }

  override val supportsK2: Boolean = true
}

data class MissingAnnotationsTherapistArgs(
  val annotations: List<Annotate>,
  val enableLogging: Boolean = true,
)

class FirMissingAnnotationsTherapistExtensionRegistrar(
  val args: MissingAnnotationsTherapistArgs,
) : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    if (args.enableLogging) {
      System.err.println("FirMissingAnnotationsTherapistExtensionRegistrar: Configuring plugin")
    }

    +FirMissingAnnotationsTherapistExtensionSessionComponent.getFactory(args)
    +::MissingAnnotationsTherapistExtensionFirCheckersExtension

    if (args.enableLogging) {
      System.err.println("FirMissingAnnotationsTherapistExtensionRegistrar: Plugin configured")
    }
  }

  init {
    if (args.enableLogging) {
      System.err.println("FirMissingAnnotationsTherapistExtensionRegistrar initialized with args: $args")
    }
  }
}

internal class FirMissingAnnotationsTherapistExtensionSessionComponent(
  session: FirSession,
  val args: MissingAnnotationsTherapistArgs,
) : FirExtensionSessionComponent(session) {

  val enableLogging: Boolean = args.enableLogging

  fun shouldAnnotateClass(declaration: FirRegularClass): Boolean {
    val should = shouldAnnotate(declaration.classId.packageFqName.asString(), detectClassType(declaration, session))
    if (enableLogging) {
      // Cannot report without a reporter; retaining the log
      println("Checking if should annotate class ${declaration.name}: $should")
    }
    return should
  }

  fun shouldAnnotateFunction(declaration: FirSimpleFunction): Boolean {
    val containingClass = declaration.getContainingClass(session)
    val packageName = containingClass?.classId?.packageFqName?.asString() ?: ""
    val should = shouldAnnotate(packageName, listOf(FunctionTypeTarget.FUNCTION))
    if (enableLogging) {
      // Cannot report without a reporter; retaining the log
      println("Checking if should annotate function ${declaration.name}: $should")
    }
    return should
  }

  fun shouldAnnotateProperty(declaration: FirProperty): Boolean {
    val containingClass = declaration.getContainingClass(session)
    val packageName = containingClass?.classId?.packageFqName?.asString() ?: ""
    val should = shouldAnnotate(packageName, listOf(PropertyTypeTarget.PROPERTY))
    if (enableLogging) {
      // Cannot report without a reporter; retaining the log
      println("Checking if should annotate property ${declaration.name}: $should")
    }
    return should
  }

  fun shouldAnnotateVariable(declaration: FirVariable): Boolean {
    val containingClass = declaration.getContainingClass(session)
    val packageName = containingClass?.classId?.packageFqName?.asString() ?: ""
    val should = shouldAnnotate(packageName, listOf(PropertyTypeTarget.LOCAL_VARIABLE))
    if (enableLogging) {
      // Cannot report without a reporter; retaining the log
      println("Checking if should annotate variable ${declaration.name}: $should")
    }
    return should
  }

  private fun shouldAnnotate(
    packageFqName: String,
    targetTypes: List<Enum<*>>,
  ): Boolean {
    val result = args.annotations.any { annotate ->
      // Check Module Target
      if (annotate.moduleTarget.isNotEmpty()) {
        // Implement module inclusion/exclusion logic if needed
        // For now, skipping moduleTarget handling
      }

      val packageFqNames = listOf(packageFqName)
      val packageMatches = packageFqNames.any { packageFqName ->
        annotate.packageTarget.any { packageTarget ->
          when (packageTarget.matchType) {
            MatchType.EXACT -> packageFqName == packageTarget.pattern
            MatchType.WILDCARD -> packageFqName.startsWith(packageTarget.pattern.removeSuffix("*"))
            MatchType.REGEX -> packageFqName.matches(Regex(packageTarget.regex ?: return@any false))
          }
        }
      }

      // Check Target Type
      val targetMatches = targetTypes.any { targetType ->
        when (targetType) {
          is ClassTypeTarget -> annotate.classTargets.contains(targetType)
          is FunctionTypeTarget -> annotate.functionTargets.contains(targetType)
          is PropertyTypeTarget -> annotate.propertyTargets.contains(targetType)
          is TypeAliasTarget -> annotate.typeAliasTargets.contains(targetType)
          is FileTarget -> annotate.fileTargets.contains(targetType)
          else -> false
        }
      }

      packageMatches && targetMatches
    }

    if (enableLogging) {
      // Cannot report without a reporter; retaining the log
      println("shouldAnnotate for packages '$packageFqName' and targets '$targetTypes': $result")
    }

    return result
  }

  companion object {
    internal fun getFactory(args: MissingAnnotationsTherapistArgs): Factory {
      return Factory { session ->
        FirMissingAnnotationsTherapistExtensionSessionComponent(session, args).also {
          if (args.enableLogging) {
            System.err.println("FirMissingAnnotationsTherapistExtensionSessionComponent created with args: $args")
          }
        }
      }
    }
  }
}

public inline val FirDeclaration.nameAsString: String
  get() = when (this) {
    is FirCallableDeclaration -> this.symbol.callableId.callableName.asString()
    is FirClass -> this.classId.shortClassName.asString()
    is FirTypeAlias -> this.name.asString()
    is FirAnonymousInitializer -> "<AnonymousInitializer>"
    is FirCodeFragment -> "<CodeFragment>"
    is FirDanglingModifierList -> "<DanglingModifierList>"
    is FirFile -> this.name
    is FirScript -> this.name.asString()
    is FirTypeParameter -> this.name.asString()
    else -> "<UnknownDeclaration>"
  }

internal val FirSession.myFirExtensionSessionComponent: FirMissingAnnotationsTherapistExtensionSessionComponent
  by FirSession.sessionComponentAccessor()

internal class MissingAnnotationsTherapistExtensionFirCheckersExtension(
  session: FirSession,
) : FirAdditionalCheckersExtension(session) {

  override val declarationCheckers: DeclarationCheckers =
    object : DeclarationCheckers() {
      override val regularClassCheckers: Set<FirRegularClassChecker> =
        setOf(MissingAnnotationsTherapistClassChecker(session))

      override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker> =
        setOf(MissingAnnotationsTherapistFunctionChecker(session))

      override val propertyCheckers: Set<FirPropertyChecker> =
        setOf(MissingAnnotationsTherapistPropertyChecker(session))
    }

  class MissingAnnotationsTherapistClassChecker(
    val session: FirSession,
  ) : FirRegularClassChecker(MppCheckerKind.Common) {

    fun annotateClassRecursively(
      declaration: FirRegularClass,
      session: FirSession,
      annotateConfig: Annotate,
      isNestedFromRecursion: Boolean = false,
      isFieldReferenced: Boolean = false,
      reporter: DiagnosticReporter,
      context: CheckerContext,

      ) {
      val component = session.myFirExtensionSessionComponent

      val isNested =
        if (isNestedFromRecursion) true
        else if (annotateConfig.annotateNestedClassesNormallyInPackage && annotateConfig.annotateNestedClassesRecursively.not()) false
        else isNestedClass(declaration, session)

      if (isNested && !annotateConfig.annotateNestedClassesRecursively) {
        if (component.enableLogging) {
          // Replace with diagnostic report
          declaration.source?.let {
            reporter.reportOn(
              it,
              COMPILER_PLUGIN_INFO,
              "Skipping annotation for nested class: ${declaration.nameAsString}",
              context,
            )
          }
        }
        return
      }

      if (isFieldReferenced && !annotateConfig.annotateFieldClassesRecursively) {
        if (component.enableLogging) {
          // Replace with diagnostic report
          declaration.source?.let {
            reporter.reportOn(
              it,
              COMPILER_PLUGIN_INFO,
              "Skipping annotation for field-referenced class: ${declaration.nameAsString}",
              context,
            )
          }
        }
        return
      }
      if (evaluateConditions(declaration, annotateConfig.conditions, session)) {
        if (component.shouldAnnotateClass(declaration)) {
          applyAnnotations(declaration, session, annotateConfig, reporter, context)
        }
      } else if (component.enableLogging) {
        // Replace with diagnostic report
        declaration.source?.let {
          reporter.reportOn(
            it,
            COMPILER_PLUGIN_INFO,
            "Conditions not met for annotating class: ${declaration.nameAsString}",
            context,
          )
        }
      }


      declaration.declarations.filterIsInstance<FirRegularClass>().forEach { innerClass ->
        annotateClassRecursively(
          innerClass,
          session,
          annotateConfig,
          isNestedFromRecursion = true,
          reporter = reporter,
          context = context,
        )
      }

      declaration.declarations.filterIsInstance<FirProperty>().forEach { property ->
        val propertyClass = getClassFromFieldType(property, session)
        if (propertyClass != null) {
          annotateClassRecursively(
            propertyClass,
            session,
            annotateConfig,
            isFieldReferenced = true,
            reporter = reporter,
            context = context,
          )
        }
      }
    }

    @OptIn(SymbolInternals::class)
    private fun getClassFromFieldType(property: FirProperty, session: FirSession): FirRegularClass? {
      val type = property.returnTypeRef.coneType
      val lookupTag = (type as? ConeClassLikeTypeImpl)?.lookupTag ?: return null
      val classSymbol = lookupTag.toSymbol(session) ?: return null
      return classSymbol.fir as? FirRegularClass
    }

    private fun applyAnnotations(
      declaration: FirRegularClass,
      session: FirSession,
      annotateConfig: Annotate,
      reporter: DiagnosticReporter,
      context: CheckerContext,
    ) {
      val existingAnnotationFqNames = declaration.annotations.mapNotNull { it.fqName(session)?.asString() }

      val newAnnotations = annotateConfig.annotationsToAdd.filter { annotation ->
        val fqName = annotation.fqName
        if (fqName in existingAnnotationFqNames) {
          if (session.myFirExtensionSessionComponent.enableLogging) {
            // Replace with diagnostic report
            declaration.source?.let {
              reporter.reportOn(
                it,
                COMPILER_PLUGIN_INFO,
                "Skipping duplicate annotation: $fqName for ${declaration.nameAsString}",
                context,
              )
            }
          }
          false
        } else {
          true
        }
      }.flatMap { it.toFirAnnotation(session, declaration) }

      if (newAnnotations.isNotEmpty()) {
        declaration.replaceAnnotations(declaration.annotations + newAnnotations)
      }
    }

    @OptIn(SymbolInternals::class, InternalDiagnosticFactoryMethod::class)
    override fun check(
      declaration: FirRegularClass,
      context: CheckerContext,
      reporter: DiagnosticReporter,

      ) {
      val component = context.session.myFirExtensionSessionComponent

      if (component.enableLogging) {
        // Replace with diagnostic report
        declaration.source?.let {
          reporter.reportOn(
            it,
            COMPILER_PLUGIN_INFO,
            "ClassChecker: Checking class '${declaration.nameAsString}' with FQCN '${declaration.symbol.fir.origin.toString()}'",
            context,
          )
        }

        declaration.source?.let {
          reporter.reportOn(
            it,
            COMPILER_PLUGIN_INFO,
            "Class details: Modality=${declaration.modality()}, Visibility=${declaration.visibility}, IsSealed=${declaration.isSealed}, IsData=${declaration.isData}",
            context,
          )
        }
      }

      val classType = detectClassType(declaration, session)
      if (component.enableLogging) {
        // Replace with diagnostic report
        declaration.source?.let {
          reporter.reportOn(
            it,
            COMPILER_PLUGIN_INFO,
            "ClassChecker: Detected class types '${classType.map { it.name }}' for class '${declaration.nameAsString}'",
            context,
          )
        }
      }

      val annotateConfigs = component.args.annotations.filter { annotate ->
        val matchesClassTarget = annotate.classTargets.any { it in classType }
        val matchesPackageTarget = annotate.packageTarget.any { packageTarget ->
          packageTarget.match(declaration.classId.packageFqName.asString())
        }
        matchesClassTarget && matchesPackageTarget
      }

      if (component.enableLogging) {
        declaration.source?.let {
          reporter.reportOn(
            it,
            COMPILER_PLUGIN_INFO,
            "ClassChecker: Found ${annotateConfigs.size} annotation configuration(s) applicable to class '${declaration.nameAsString}'",
            context,
          )
        }
        annotateConfigs.forEachIndexed { index, config ->
          declaration.source?.let {
            reporter.reportOn(
              it,
              COMPILER_PLUGIN_INFO,
              "ClassChecker: Applying annotation config #${index + 1}: Targets=${config.classTargets}, Annotations=${config.annotationsToAdd.map { it.fqName }}",
              context,
            )
          }
        }
      }

      annotateConfigs.forEach { config ->
        try {
          annotateClassRecursively(declaration, context.session, config, reporter = reporter, context = context)
          if (component.enableLogging) {
            declaration.source?.let { reporter.reportOn(it, COMPILER_PLUGIN_INFO, "", context) }

            declaration.source?.let {
              reporter.reportOn(
                it,
                COMPILER_PLUGIN_INFO,
                "ClassChecker: Successfully applied annotations for config: ${config.annotationsToAdd.map { it.fqName }}",
                context,
              )
            }
          }
        } catch (e: Exception) {
          if (component.enableLogging) {
            declaration.source?.let {
              reporter.reportOn(
                it,
                COMPILER_PLUGIN_INFO,
                "ClassChecker: Failed to apply annotations for config: ${config.annotationsToAdd.map { it.fqName }}. Error: ${e.message}",
                context,
              )
            }
          }
        }
      }

      if (component.enableLogging) {
        declaration.source?.let {
          reporter.reportOn(
            it,
            COMPILER_PLUGIN_INFO,
            "ClassChecker: Completed checking class '${declaration.nameAsString}'",
            context,
          )
        }
      }
    }
  }

  class MissingAnnotationsTherapistFunctionChecker(
    val session: FirSession,
  ) : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    override fun check(
      declaration: FirSimpleFunction,
      context: CheckerContext,
      reporter: DiagnosticReporter,
    ) {
      val component = context.session.myFirExtensionSessionComponent

      if (component.enableLogging) {
        // Replace with diagnostic report
        declaration.source?.let {
          reporter.reportOn(
            it,
            COMPILER_PLUGIN_INFO,
            "FunctionChecker: Checking function ${declaration.name}",
            context,
          )
        }
      }

      if (!component.shouldAnnotateFunction(declaration)) {
        if (component.enableLogging) {
          // Replace with diagnostic report
          declaration.source?.let {
            reporter.reportOn(
              it,
              COMPILER_PLUGIN_INFO,
              "FunctionChecker: Skipping annotation for function ${declaration.name}",
              context,
            )
          }
        }
        return
      }

      val annotateConfigs = context.session.myFirExtensionSessionComponent.args.annotations
        .filter { annotate ->
          annotate.functionTargets.isNotEmpty() &&
            annotate.packageTarget.any { packageTarget ->
              when (packageTarget.matchType) {
                MatchType.EXACT -> declaration.getContainingClass(session)?.classId?.packageFqName?.asString() == packageTarget.pattern
                MatchType.WILDCARD -> declaration.getContainingClass(session)?.classId?.packageFqName?.asString()
                  ?.startsWith(packageTarget.pattern.removeSuffix("*")) ?: false

                MatchType.REGEX -> declaration.getContainingClass(session)?.classId?.packageFqName?.asString()
                  ?.matches(Regex(packageTarget.regex ?: return@any false)) ?: false
              }
            }
        }

      if (annotateConfigs.isEmpty()) {
        if (component.enableLogging) {
          // Replace with diagnostic report
          declaration.source?.let {
            reporter.reportOn(
              it,
              COMPILER_PLUGIN_INFO,
              "FunctionChecker: No annotation configurations matched for function ${declaration.name}",
              context,
            )
          }
        }
        return
      }


      annotateConfigs.forEach { annotate ->
        if (evaluateConditions(declaration, annotate.conditions, session)) {
          if (component.enableLogging) {
            // Replace with diagnostic report
            declaration.source?.let {
              reporter.reportOn(
                it,
                COMPILER_PLUGIN_INFO,
                "FunctionChecker: Applying annotations to function ${declaration.name}: ${annotate.annotationsToAdd}",
                context,
              )
            }
          }
          val newAnnotations = annotate.annotationsToAdd.flatMap { it.toFirAnnotation(session, declaration) }
          declaration.replaceAnnotations(declaration.annotations + newAnnotations)
        } else {
          if (component.enableLogging) {
            // Replace with diagnostic report
            declaration.source?.let {
              reporter.reportOn(
                it,
                COMPILER_PLUGIN_INFO,
                "FunctionChecker: Conditions not met for annotations on function ${declaration.name}",
                context,
              )
            }
          }
        }
      }
    }
  }

  class MissingAnnotationsTherapistPropertyChecker(
    val session: FirSession,
  ) : FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(
      declaration: FirProperty,
      context: CheckerContext,
      reporter: DiagnosticReporter,
    ) {
      val component = context.session.myFirExtensionSessionComponent

      if (component.enableLogging) {
        // Replace with diagnostic report
        declaration.source?.let {
          reporter.reportOn(
            it,
            COMPILER_PLUGIN_INFO,
            "PropertyChecker: Checking property ${declaration.name}",
            context,
          )
        }
      }

      if (!component.shouldAnnotateProperty(declaration)) {
        if (component.enableLogging) {
          // Replace with diagnostic report
          declaration.source?.let {
            reporter.reportOn(
              it,
              COMPILER_PLUGIN_INFO,
              "PropertyChecker: Skipping annotation for property ${declaration.name}",
              context,
            )
          }
        }
        return
      }

      val annotateConfigs = context.session.myFirExtensionSessionComponent.args.annotations
        .filter { annotate ->
          annotate.propertyTargets.isNotEmpty() &&
            annotate.packageTarget.any { packageTarget ->
              when (packageTarget.matchType) {
                MatchType.EXACT -> declaration.getContainingClass(session)?.classId?.packageFqName?.asString() == packageTarget.pattern
                MatchType.WILDCARD -> declaration.getContainingClass(session)?.classId?.packageFqName?.asString()
                  ?.startsWith(packageTarget.pattern.removeSuffix("*")) ?: false

                MatchType.REGEX -> declaration.getContainingClass(session)?.classId?.packageFqName?.asString()
                  ?.matches(Regex(packageTarget.regex ?: return@any false)) ?: false
              }
            }
        }

      if (annotateConfigs.isEmpty()) {
        if (component.enableLogging) {
          // Replace with diagnostic report
          declaration.source?.let {
            reporter.reportOn(
              it,
              COMPILER_PLUGIN_INFO,
              "PropertyChecker: No annotation configurations matched for property ${declaration.name}",
              context,
            )
          }
        }
        return
      }


      annotateConfigs.forEach { annotate ->
        if (evaluateConditions(declaration, annotate.conditions, session)) {
          if (component.enableLogging) {
            // Replace with diagnostic report
            declaration.source?.let {
              reporter.reportOn(
                it,
                COMPILER_PLUGIN_INFO,
                "PropertyChecker: Applying annotations to property ${declaration.name}: ${annotate.annotationsToAdd}",
                context,
              )
            }
          }
          val newAnnotations = annotate.annotationsToAdd.flatMap { it.toFirAnnotation(session, declaration) }
          declaration.replaceAnnotations(declaration.annotations + newAnnotations)
        } else {
          if (component.enableLogging) {
            // Replace with diagnostic report
            declaration.source?.let {
              reporter.reportOn(
                it,
                COMPILER_PLUGIN_INFO,
                "PropertyChecker: Conditions not met for annotations on property ${declaration.name}",
                context,
              )
            }
          }
        }
      }
    }
  }
}

fun PackageTarget.match(packageName: String): Boolean {
  return when (matchType) {
    MatchType.EXACT -> packageName == pattern
    MatchType.WILDCARD -> packageName.startsWith(pattern.removeSuffix("*"))
    MatchType.REGEX -> regex?.let { Regex(it).matches(packageName) } ?: false
  }
}

@OptIn(SymbolInternals::class)
private fun <T : FirDeclaration> evaluateConditions(
  declaration: T,
  conditions: List<Condition>,
  session: FirSession,
): Boolean {
  val component = session.myFirExtensionSessionComponent
  if (component.enableLogging) {
    // Replace with diagnostic report
    // Note: Without a reporter, cannot replace this log
    // Retaining the log
    println("Evaluating conditions for declaration: ${declaration.nameAsString}")
  }
  return conditions.all { condition ->

    val existingCheck = condition.existingAnnotations.all { existing ->
      declarationHasAnnotation(declaration, existing, session)
    }

    val absenceCheck = condition.annotationsAbsence.none { absent ->
      declarationHasAnnotation(declaration, absent, session)
    }

    val namePatternCheck = condition.namePattern?.let { pattern ->
      val name = declaration.nameAsString
      val matches = Regex(pattern).matches(name)
      if (component.enableLogging) {
        // Replace with diagnostic report
        // Cannot report without a reporter; retaining the log
        println("NamePatternCheck for '$name' with pattern '$pattern': $matches")
      }
      matches
    } ?: true

    val visibilityCheck = condition.visibility?.let { visibility ->
      val actualVisibility = declaration.visibilityForApproximation(declaration)
      val matches = when (visibility) {
        Visibility.PUBLIC -> actualVisibility == Visibilities.Public
        Visibility.PRIVATE -> actualVisibility == Visibilities.Private
        Visibility.PROTECTED -> actualVisibility == Visibilities.Protected
        Visibility.INTERNAL -> actualVisibility == Visibilities.Internal
      }
      if (component.enableLogging) {
        // Replace with diagnostic report
        // Cannot report without a reporter; retaining the log
        println("VisibilityCheck for '${declaration.nameAsString}' with visibility '$visibility': $matches")
      }
      matches
    } ?: true

    val modifiersCheck = condition.modifiers?.let { requiredModifiers ->
      val matches = requiredModifiers.all { modifier ->
        hasModifier(declaration, modifier)
      }
      if (component.enableLogging) {
        // Replace with diagnostic report
        // Cannot report without a reporter; retaining the log
        println("ModifiersCheck for '${declaration.nameAsString}' with required modifiers '$requiredModifiers': $matches")
      }
      matches
    } ?: true

    val inheritanceCheck = condition.inheritance?.let { inheritance ->
      val superclassFqName = inheritance.superclass?.toClassId()
      val actualSuperclasses = when (declaration) {
        is FirRegularClass ->
          declaration.superConeTypes.map { it }
            .mapNotNull { it.lookupTag.toSymbol(session)?.fir?.classId }

        else -> emptyList()
      }
      val matches = superclassFqName?.let { actualSuperclasses.contains(it) } ?: true
      if (component.enableLogging) {
        // Replace with diagnostic report
        // Cannot report without a reporter; retaining the log
        println("InheritanceCheck for '${declaration.nameAsString}' with superclass '$superclassFqName': $matches")
      }
      matches
    } ?: true

    val typeConditionCheck = condition.typeCondition?.let { typeCondition ->
      val typeNames = typeCondition.typeNames
      val actualTypeName = when (declaration) {
        is FirRegularClass -> declaration.symbol.classId.asString()
        is FirProperty -> declaration.returnTypeRef.coneType?.toString() ?: ""
        is FirSimpleFunction -> declaration.returnTypeRef.coneType?.toString() ?: ""
        else -> ""
      }
      val matches = typeNames.any { typeName ->
        actualTypeName.contains(typeName)
      }
      if (component.enableLogging) {
        // Replace with diagnostic report
        // Cannot report without a reporter; retaining the log
        println("TypeConditionCheck for '${declaration.nameAsString}' with type names '$typeNames': $matches")
      }
      matches
    } ?: true

    val customPredicateCheck = true/*condition.customPredicate?.invoke(declaration) ?: true*/

    val allConditionsMet = existingCheck &&
      absenceCheck &&
      namePatternCheck &&
      visibilityCheck &&
      modifiersCheck &&
      inheritanceCheck &&
      typeConditionCheck &&
      customPredicateCheck

    if (component.enableLogging) {
      // Replace with diagnostic report
      // Cannot report without a reporter; retaining the log
      println("All conditions met for '${declaration.nameAsString}': $allConditionsMet")
    }

    allConditionsMet
  }
}

fun Modifier.toKtModifierKeywordToken(): KtKeywordToken = when (this) {
  Modifier.ABSTRACT -> KtTokens.ABSTRACT_KEYWORD
  Modifier.OPEN -> KtTokens.OPEN_KEYWORD
  Modifier.FINAL -> KtTokens.FINAL_KEYWORD
  Modifier.SEALED -> KtTokens.SEALED_KEYWORD
  Modifier.DATA -> KtTokens.DATA_KEYWORD
  Modifier.ENUM -> KtTokens.ENUM_KEYWORD
  Modifier.INTERFACE -> KtTokens.INTERFACE_KEYWORD
  Modifier.OBJECT -> KtTokens.OBJECT_KEYWORD
  Modifier.COMPANION -> KtTokens.COMPANION_KEYWORD
  Modifier.CONST -> KtTokens.CONST_KEYWORD
  Modifier.LATEINIT -> KtTokens.LATEINIT_KEYWORD
  Modifier.INLINE -> KtTokens.INLINE_KEYWORD
  Modifier.NOINLINE -> KtTokens.NOINLINE_KEYWORD
  Modifier.CROSSINLINE -> KtTokens.CROSSINLINE_KEYWORD
  Modifier.REIFIED -> KtTokens.REIFIED_KEYWORD
  Modifier.TAILREC -> KtTokens.TAILREC_KEYWORD
  Modifier.SUSPEND -> KtTokens.SUSPEND_KEYWORD
  Modifier.OPERATOR -> KtTokens.OPERATOR_KEYWORD
  Modifier.INFIX -> KtTokens.INFIX_KEYWORD
  Modifier.EXTERNAL -> KtTokens.EXTERNAL_KEYWORD
  Modifier.ANNOTATION -> KtTokens.ANNOTATION_KEYWORD
  Modifier.VARARG -> KtTokens.VARARG_KEYWORD
  Modifier.OVERRIDE -> KtTokens.OVERRIDE_KEYWORD
  Modifier.EXPECT -> KtTokens.EXPECT_KEYWORD
  Modifier.ACTUAL -> KtTokens.ACTUAL_KEYWORD
  Modifier.PUBLIC -> KtTokens.PUBLIC_KEYWORD
  Modifier.PRIVATE -> KtTokens.PRIVATE_KEYWORD
  Modifier.PROTECTED -> KtTokens.PROTECTED_KEYWORD
  Modifier.INTERNAL -> KtTokens.INTERNAL_KEYWORD
}

private fun hasModifier(declaration: FirDeclaration, modifier: Modifier): Boolean {
  fun FirElement.hasModifier(token: KtKeywordToken): Boolean =
    token as KtModifierKeywordToken in source.getModifierList()

  return declaration.hasModifier(modifier.toKtModifierKeywordToken())
}

private fun <T> declarationHasAnnotation(declaration: T, fqName: String, session: FirSession): Boolean {

  return when (declaration) {
    is FirRegularClass -> {
      val has = declaration.hasAnnotation(fqName.toClassId(), session)
      if (session.myFirExtensionSessionComponent.enableLogging) {
        // Replace with diagnostic report
        // Cannot report without a reporter; retaining the log
        println("DeclarationHasAnnotation: Class ${declaration.name} has annotation $fqName: $has")
      }
      has
    }

    is FirSimpleFunction -> {
      val has = declaration.hasAnnotation(fqName.toClassId(), session)
      if (session.myFirExtensionSessionComponent.enableLogging) {
        // Replace with diagnostic report
        // Cannot report without a reporter; retaining the log
        println("DeclarationHasAnnotation: Function ${declaration.name} has annotation $fqName: $has")
      }
      has
    }

    is FirProperty -> {
      val has = declaration.hasAnnotation(fqName.toClassId(), session)
      if (session.myFirExtensionSessionComponent.enableLogging) {
        // Replace with diagnostic report
        // Cannot report without a reporter; retaining the log
        println("DeclarationHasAnnotation: Property ${declaration.name} has annotation $fqName: $has")
      }
      has
    }

    is FirVariable -> {
      val has = declaration.hasAnnotation(fqName.toClassId(), session)
      if (session.myFirExtensionSessionComponent.enableLogging) {
        // Replace with diagnostic report
        // Cannot report without a reporter; retaining the log
        println("DeclarationHasAnnotation: Variable ${declaration.name} has annotation $fqName: $has")
      }
      has
    }

    else -> {
      if (session.myFirExtensionSessionComponent.enableLogging) {
        // Replace with diagnostic report
        // Cannot report without a reporter; retaining the log
        println("DeclarationHasAnnotation: Unknown declaration type for $declaration")
      }
      false
    }
  }
}

private fun Annotation.toFirAnnotation(
  session: FirSession,
  declaration: FirDeclaration,
): List<FirAnnotation> {


  return listOf(
    createFirAnnotation(
      fqName = this.toFqName(),
      parameters = /*parameters?:*/ emptyMap(),
      declarationName = declaration.nameAsString,
      declarationSource = declaration.source
        ?: error("Declaration source for {${declaration.nameAsString}} must not be null"),
      session = session,
    ),
  )
}

private fun dev.shalaga44.mat.Annotation.toFqName(): FqName {
  return FqName(this.fqName)
}

fun createFirAnnotation(
  fqName: FqName,
  parameters: Map<String, String>,
  declarationName: String,
  declarationSource: KtSourceElement,
  session: FirSession,
): FirAnnotation = buildAnnotation {

  annotationTypeRef = buildResolvedTypeRef {
    type = ConeClassLikeTypeImpl(
      lookupTag = ConeClassLikeLookupTagImpl(ClassId.topLevel(fqName)),
      typeArguments = emptyArray(),
      isNullable = false,
    )
  }

  val parametersFinal: Map<String, String> = parameters
  argumentMapping = if (false/*parametersFinal.isNotEmpty()*/) {
    buildAnnotationArgumentMapping {
      parametersFinal.forEach { (key, value) ->
        val processedValue = value.replace("{className}", declarationName)
        val name = Name.identifier(key)
        mapping[name] = buildLiteralExpression(
          source = declarationSource,
          kind = org.jetbrains.kotlin.types.ConstantValueKind.String,
          value = processedValue,
          setType = true,
        )
      }
    }
  } else {
    FirAnnotationArgumentMappingImpl(
      source,
      emptyMap(),
    )
  }

}

public inline fun String.toClassId(): ClassId = FqName(this).run { ClassId(parent(), shortName()) }

private fun isNestedClass(declaration: FirRegularClass, session: FirSession): Boolean {
  val containingClass = declaration.symbol.getContainingClassSymbol(session)
  return containingClass != null
}

fun detectClassType(declaration: FirRegularClass, session: FirSession): List<ClassTypeTarget> {
  val classTypes = mutableListOf<ClassTypeTarget>()
  val notRegularClass = listOf(
    ClassTypeTarget.INLINE_CLASS,
    ClassTypeTarget.VALUE_CLASS,
    ClassTypeTarget.EXPECT_CLASS,
    ClassTypeTarget.ACTUAL_CLASS,
    ClassTypeTarget.ABSTRACT_CLASS,

    )


  if (declaration.classKind == ClassKind.OBJECT) {
    if (declaration.classId.isLocal) {
      classTypes.add(ClassTypeTarget.OBJECT_CLASS)
    } else {
      classTypes.add(ClassTypeTarget.OBJECT_CLASS)
    }
  }


  if (declaration.classId.isLocal) {
    classTypes.add(ClassTypeTarget.LOCAL_CLASS)
  }


  if (declaration.isEnumClass) {
    classTypes.add(ClassTypeTarget.ENUM_CLASS)
  }
  if (declaration.isAbstract) {
    classTypes.add(ClassTypeTarget.ABSTRACT_CLASS)
  }


  if (declaration.isSealed) {
    classTypes.add(ClassTypeTarget.SEALED_CLASS)
  }


  if (declaration.isData) {
    classTypes.add(ClassTypeTarget.DATA_CLASS)
  }


  if (declaration.classKind == ClassKind.ANNOTATION_CLASS) {
    classTypes.add(ClassTypeTarget.ANNOTATION_CLASS)
  }


  if (declaration.isInterface) {
    classTypes.add(ClassTypeTarget.INTERFACE_CLASS)
  }


  if (declaration.isCompanion) {
    classTypes.add(ClassTypeTarget.COMPANION_OBJECT_CLASS)
  }


  if (declaration.isInner) {
    classTypes.add(ClassTypeTarget.INNER_CLASS).also { classTypes.add(ClassTypeTarget.REGULAR_CLASS) }
  }
  if (declaration.isExpect || containsExpectModifier(declaration)) {
    classTypes.add(ClassTypeTarget.EXPECT_CLASS)
  }
  if (declaration.isActual || containsActualModifier(declaration)) {
    classTypes.add(ClassTypeTarget.ACTUAL_CLASS)
  }
  if (declaration.isInline) {
    classTypes.add(ClassTypeTarget.INLINE_CLASS)
  }

  with(declaration) {
    if (isInline && name != StandardClassIds.Result.shortClassName) {
      classTypes.add(ClassTypeTarget.VALUE_CLASS)
    }
  }


  if (isNestedClass(declaration, session)) {
    classTypes.add(ClassTypeTarget.NESTED_CLASS).also { classTypes.add(ClassTypeTarget.REGULAR_CLASS) }

  }

  when (declaration.modality()) {
    Modality.ABSTRACT -> classTypes.add(ClassTypeTarget.ABSTRACT_CLASS)

    Modality.OPEN -> classTypes.add(ClassTypeTarget.OPEN_CLASS)
      .also { if (classTypes.all { it !in notRegularClass }) classTypes.add(ClassTypeTarget.REGULAR_CLASS) }

    Modality.FINAL -> classTypes.add(ClassTypeTarget.FINAL_CLASS)
      .also { if (classTypes.all { it !in notRegularClass }) classTypes.add(ClassTypeTarget.REGULAR_CLASS) }


    else -> {

    }
  }



  if (classTypes.isEmpty()) {
    classTypes.add(ClassTypeTarget.REGULAR_CLASS)
  }

  return classTypes.distinct()
}

private fun containsExpectModifier(declaration: FirMemberDeclaration): Boolean {
  return declaration.source.getModifierList()?.let { modifiers ->
    KtTokens.EXPECT_KEYWORD in modifiers
  } ?: false
}

private fun containsActualModifier(declaration: FirMemberDeclaration): Boolean {
  return declaration.source.getModifierList()?.let { modifiers ->
    KtTokens.ACTUAL_KEYWORD in modifiers
  } ?: false
}

object CustomFirErrors {

  val COMPILER_PLUGIN_INFO: KtDiagnosticFactory1<String> =
    KtDiagnosticFactory1(
      name = "COMPILER_PLUGIN_INFO",
      severity = Severity.WARNING,
      defaultPositioningStrategy = SourceElementPositioningStrategies.DEFAULT,
      psiType = KtElement::class,
    )
}
