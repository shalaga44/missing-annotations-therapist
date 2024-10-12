package com.shalaga44.annotations

import com.google.auto.service.AutoService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shalaga44.annotations.utils.Annotate
import com.shalaga44.annotations.utils.Annotation
import com.shalaga44.annotations.utils.Condition
import com.shalaga44.annotations.utils.MatchType
import com.shalaga44.annotations.utils.Modifier
import com.shalaga44.annotations.utils.Visibility
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.FirAnnotationBuilder
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.visibilityForApproximation
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName


@AutoService(CompilerPluginRegistrar::class)
class FirMissingAnnotationsTherapistCompilerPluginRegistrar(
  private val args: MissingAnnotationsTherapistArgs? = null,
) : CompilerPluginRegistrar() {

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val configJson = configuration[KEY_CONFIG] ?: "[]"
    val enableLogging = args?.enableLogging ?: configuration[KEY_ENABLE_LOGGING]?.toBoolean() ?: false

    if (enableLogging) {
      System.err.println("FirMissingAnnotationsTherapistCompilerPluginRegistrar: Starting registration")
      System.err.println("Configuration JSON: $configJson")
      System.err.println("Logging is enabled")
    }

    val gson = Gson()
    val type = object : TypeToken<List<Annotate>>() {}.type
    val annotations: List<Annotate>? = gson.fromJson<List<Annotate>?>(configJson, type).ifEmpty { null }
    val pluginArgs = args ?: MissingAnnotationsTherapistArgs(annotations ?: return, enableLogging)

    if (enableLogging) {
      System.err.println("Parsed annotations: $annotations")
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
    val should = shouldAnnotate(declaration.classId.packageFqName.asString())
    if (enableLogging) {
      println("Checking if should annotate class ${declaration.name}: $should")
    }
    return should
  }

  fun shouldAnnotateFunction(declaration: FirSimpleFunction): Boolean {
    val containingClass = declaration.getContainingClass(session)
    val should = shouldAnnotate(containingClass?.classId?.packageFqName?.asString() ?: "")
    if (enableLogging) {
      println("Checking if should annotate function ${declaration.name}: $should")
    }
    return should
  }

  fun shouldAnnotateProperty(declaration: FirProperty): Boolean {
    val containingClass = declaration.getContainingClass(session)
    val should = shouldAnnotate(containingClass?.classId?.packageFqName?.asString() ?: "")
    if (enableLogging) {
      println("Checking if should annotate property ${declaration.name}: $should")
    }
    return should
  }

  fun shouldAnnotateVariable(declaration: FirVariable): Boolean {
    val containingClass = declaration.getContainingClass(session)
    val should = shouldAnnotate(containingClass?.classId?.packageFqName?.asString() ?: "")
    if (enableLogging) {
      println("Checking if should annotate variable ${declaration.name}: $should")
    }
    return should
  }

  private fun shouldAnnotate(packageFqName: String): Boolean {
    val result = args.annotations.any { annotate ->
      annotate.packageTarget.any { packageTarget ->
        when (packageTarget.matchType) {
          MatchType.EXACT -> packageFqName == packageTarget.pattern
          MatchType.WILDCARD -> packageFqName.startsWith(packageTarget.pattern.removeSuffix("*"))
          MatchType.REGEX -> packageFqName.matches(Regex(packageTarget.regex ?: return@any false))
        }
      }
    }
    if (enableLogging) {
      println("shouldAnnotate for package '$packageFqName': $result")
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

      /*
            override val variableCheckers: Set<FirVariableChecker> =
              setOf(MissingAnnotationsTherapistVariableChecker)
      */
    }

  class MissingAnnotationsTherapistClassChecker(
    val session: FirSession,
  ) : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(
      declaration: FirRegularClass,
      context: CheckerContext,
      reporter: DiagnosticReporter,
    ) {
      val component = context.session.myFirExtensionSessionComponent

      if (component.enableLogging) {
        println("ClassChecker: Checking class ${declaration.name}")
      }

      if (!component.shouldAnnotateClass(declaration)) {
        if (component.enableLogging) {
          println("ClassChecker: Skipping annotation for class ${declaration.name}")
        }
        return
      }

      val annotateConfigs = context.session.myFirExtensionSessionComponent.args.annotations
        .filter { annotate ->
          annotate.annotationsTarget.contains(AnnotationTarget.CLASS) &&
            annotate.packageTarget.any { packageTarget ->
              when (packageTarget.matchType) {
                MatchType.EXACT -> declaration.classId.packageFqName.asString() == packageTarget.pattern
                MatchType.WILDCARD -> declaration.classId.packageFqName.asString()
                  .startsWith(packageTarget.pattern.removeSuffix("*"))

                MatchType.REGEX -> declaration.classId.packageFqName.asString()
                  .matches(Regex(packageTarget.regex ?: return@any false))
              }
            }
        }

      if (annotateConfigs.isEmpty()) {
        if (component.enableLogging) {
          println("ClassChecker: No annotation configurations matched for class ${declaration.name}")
        }
        return
      }


      annotateConfigs.forEach { annotate ->
        if (evaluateConditions(declaration, annotate.conditions, context, session)) {
          if (component.enableLogging) {
            println("ClassChecker: Applying annotations to class ${declaration.name}: ${annotate.annotationsToAdd}")
          }
          val newAnnotations = annotate.annotationsToAdd.flatMap { it.toFirAnnotation() }
          declaration.replaceAnnotations(declaration.annotations + newAnnotations)
        } else {
          if (component.enableLogging) {
            println("ClassChecker: Conditions not met for annotations on class ${declaration.name}")
          }
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
        println("FunctionChecker: Checking function ${declaration.name}")
      }

      if (!component.shouldAnnotateFunction(declaration)) {
        if (component.enableLogging) {
          println("FunctionChecker: Skipping annotation for function ${declaration.name}")
        }
        return
      }

      val annotateConfigs = context.session.myFirExtensionSessionComponent.args.annotations
        .filter { annotate ->
          annotate.annotationsTarget.contains(AnnotationTarget.FUNCTION) &&
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
          println("FunctionChecker: No annotation configurations matched for function ${declaration.name}")
        }
        return
      }


      annotateConfigs.forEach { annotate ->
        if (evaluateConditions(declaration, annotate.conditions, context, session)) {
          if (component.enableLogging) {
            println("FunctionChecker: Applying annotations to function ${declaration.name}: ${annotate.annotationsToAdd}")
          }
          val newAnnotations = annotate.annotationsToAdd.flatMap { it.toFirAnnotation() }
          declaration.replaceAnnotations(declaration.annotations + newAnnotations)
        } else {
          if (component.enableLogging) {
            println("FunctionChecker: Conditions not met for annotations on function ${declaration.name}")
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
        println("PropertyChecker: Checking property ${declaration.name}")
      }

      if (!component.shouldAnnotateProperty(declaration)) {
        if (component.enableLogging) {
          println("PropertyChecker: Skipping annotation for property ${declaration.name}")
        }
        return
      }

      val annotateConfigs = context.session.myFirExtensionSessionComponent.args.annotations
        .filter { annotate ->
          annotate.annotationsTarget.contains(AnnotationTarget.PROPERTY) &&
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
          println("PropertyChecker: No annotation configurations matched for property ${declaration.name}")
        }
        return
      }


      annotateConfigs.forEach { annotate ->
        if (evaluateConditions(declaration, annotate.conditions, context, session)) {
          if (component.enableLogging) {
            println("PropertyChecker: Applying annotations to property ${declaration.name}: ${annotate.annotationsToAdd}")
          }
          val newAnnotations = annotate.annotationsToAdd.flatMap { it.toFirAnnotation() }
          declaration.replaceAnnotations(declaration.annotations + newAnnotations)
        } else {
          if (component.enableLogging) {
            println("PropertyChecker: Conditions not met for annotations on property ${declaration.name}")
          }
        }
      }
    }
  }

  /*
    object MissingAnnotationsTherapistVariableChecker : FirVariableChecker(MppCheckerKind.Common) {
      override fun check(
        declaration: FirVariable,
        context: CheckerContext,
        reporter: DiagnosticReporter,
      ) {
        val component = context.session.myFirExtensionSessionComponent

        if (component.enableLogging) {
          println("VariableChecker: Checking variable ${declaration.nameAsString}")
        }

        if (!component.shouldAnnotateVariable(declaration)) {
          if (component.enableLogging) {
            println("VariableChecker: Skipping annotation for variable ${declaration.nameAsString}")
          }
          return
        }

        val annotateConfigs = context.session.myFirExtensionSessionComponent.args.annotations
          .filter { annotate ->
            annotate.annotationsTarget.contains(AnnotationTarget.LOCAL_VARIABLE) &&
              annotate.packageTarget.any { packageTarget ->
                when (packageTarget.matchType) {
                  MatchType.EXACT -> declaration.nameAsString.asString().startsWith(packageTarget.pattern)
                  MatchType.WILDCARD -> declaration.nameAsString.asString()
                    .startsWith(packageTarget.pattern.removeSuffix("*"))
                  MatchType.REGEX -> declaration.nameAsString.asString()
                    .matches(Regex(packageTarget.regex ?: return@filter false))
                }
              }
          }

        if (annotateConfigs.isEmpty()) {
          if (component.enableLogging) {
            println("VariableChecker: No annotation configurations matched for variable ${declaration.nameAsString}")
          }
          return
        }

        
        annotateConfigs.forEach { annotate ->
          if (evaluateConditions(declaration, annotate.conditions, context)) {
            if (component.enableLogging) {
              println("VariableChecker: Applying annotations to variable ${declaration.nameAsString}: ${annotate.annotationsToAdd}")
            }
            val newAnnotations = annotate.annotationsToAdd.flatMap { it.toFirAnnotation() }
            declaration.replaceAnnotations(declaration.annotations + newAnnotations)
          } else {
            if (component.enableLogging) {
              println("VariableChecker: Conditions not met for annotations on variable ${declaration.nameAsString}")
            }
          }
        }
      }
    }
  */

}

@OptIn(SymbolInternals::class)
private fun <T : FirDeclaration> evaluateConditions(
  declaration: T,
  conditions: List<Condition>,
  context: CheckerContext,
  session: FirSession,
): Boolean {
  val component = context.session.myFirExtensionSessionComponent
  if (component.enableLogging) {
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
        println("VisibilityCheck for '${declaration.nameAsString}' with visibility '$visibility': $matches")
      }
      matches
    } ?: true


    val modifiersCheck = condition.modifiers?.let { requiredModifiers ->
      val matches = requiredModifiers.all { modifier ->
        hasModifier(declaration, modifier)
      }
      if (component.enableLogging) {
        println("ModifiersCheck for '${declaration.nameAsString}' with required modifiers '$requiredModifiers': $matches")
      }
      matches
    } ?: true


    val inheritanceCheck = condition.inheritance?.let { inheritance ->
      val superclassFqName = inheritance.superclass?.toClassId()
      val actualSuperclasses = when (declaration) {
        is FirRegularClass ->
          declaration.superConeTypes.map { it }
            .map { it.lookupTag.toSymbol(session)?.fir?.classId }

        else -> emptyList()
      }
      val matches = actualSuperclasses.contains(superclassFqName)
      if (component.enableLogging) {
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
      println("All conditions met for '${declaration.nameAsString}': $allConditionsMet")
    }

    allConditionsMet
  }
}

fun Modifier.toKtModifierKeywordToken(): KtModifierKeywordToken = when (this) {
  Modifier.OPEN -> KtTokens.OPEN_KEYWORD
  Modifier.FINAL -> KtTokens.FINAL_KEYWORD
  Modifier.ABSTRACT -> KtTokens.ABSTRACT_KEYWORD
  Modifier.SUSPEND -> KtTokens.SUSPEND_KEYWORD
  Modifier.PRIVATE -> KtTokens.PRIVATE_KEYWORD
}


private fun hasModifier(declaration: FirDeclaration, modifier: Modifier): Boolean {
  return declaration.hasModifier(modifier.toKtModifierKeywordToken())
}

private fun <T> declarationHasAnnotation(declaration: T, fqName: String, session: FirSession): Boolean {

  return when (declaration) {
    is FirRegularClass -> {
      val has = declaration.hasAnnotation(fqName.toClassId(), session)
      println("DeclarationHasAnnotation: Class ${declaration.name} has annotation $fqName: $has")
      has
    }

    is FirSimpleFunction -> {
      val has = declaration.hasAnnotation(fqName.toClassId(), session)
      println("DeclarationHasAnnotation: Function ${declaration.name} has annotation $fqName: $has")
      has
    }

    is FirProperty -> {
      val has = declaration.hasAnnotation(fqName.toClassId(), session)
      println("DeclarationHasAnnotation: Property ${declaration.name} has annotation $fqName: $has")
      has
    }

    is FirVariable -> {
      val has = declaration.hasAnnotation(fqName.toClassId(), session)
      println("DeclarationHasAnnotation: Variable ${declaration.name} has annotation $fqName: $has")
      has
    }

    else -> {
      println("DeclarationHasAnnotation: Unknown declaration type for $declaration")
      false
    }
  }
}

private fun Annotation.toFirAnnotation(): List<FirAnnotation> {
  return listOf(createFirAnnotation(this.toFqName()))
}

private fun Annotation.toFqName(): FqName {
  return FqName(this.fqName)
}


fun createFirAnnotation(
  fqName: FqName,
  argumentMapping: FirAnnotationArgumentMapping = FirEmptyAnnotationArgumentMapping,
  builder: FirAnnotationBuilder.() -> Unit = {},
): FirAnnotation = buildAnnotation {
  this.annotationTypeRef = buildResolvedTypeRef {
    type = ConeClassLikeTypeImpl(
      lookupTag = ConeClassLikeLookupTagImpl(ClassId.topLevel(fqName)),
      typeArguments = emptyArray(),
      isNullable = false,
    )
  }
  this.argumentMapping = argumentMapping
  builder()
}

public inline fun String.toClassId(): ClassId = FqName(this).run { ClassId(parent(), shortName()) }


