package dev.shalaga44.mat

import com.google.auto.service.AutoService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.shalaga44.mat.utils.Annotate
import dev.shalaga44.mat.utils.Annotation
import dev.shalaga44.mat.utils.Condition
import dev.shalaga44.mat.utils.MatchType
import dev.shalaga44.mat.utils.Modifier
import dev.shalaga44.mat.utils.PackageTarget
import dev.shalaga44.mat.utils.Visibility
import org.jetbrains.kotlin.KtSourceElement
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
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
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
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
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
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


@AutoService(CompilerPluginRegistrar::class)
class MissingAnnotationsTherapistCompilerPluginRegistrar(
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
    ) {
      val component = session.myFirExtensionSessionComponent

      val isNested = if (isNestedFromRecursion) true else isNestedClass(declaration, session)

      if (isNested && !annotateConfig.annotateNestedClasses) {
        if (component.enableLogging) {
          println("Skipping annotation for nested class: ${declaration.nameAsString}")
        }
        return
      }

      if (isFieldReferenced && !annotateConfig.annotateFieldClasses) {
        if (component.enableLogging) {
          println("Skipping annotation for field-referenced class: ${declaration.nameAsString}")
        }
        return
      }

      if (component.shouldAnnotateClass(declaration)) {
        applyAnnotations(declaration, session, annotateConfig)
      }

      declaration.declarations.filterIsInstance<FirRegularClass>().forEach { innerClass ->
        annotateClassRecursively(innerClass, session, annotateConfig, isNestedFromRecursion = true)
      }

      declaration.declarations.filterIsInstance<FirProperty>().forEach { property ->
        val propertyClass = getClassFromFieldType(property, session)
        if (propertyClass != null) {
          annotateClassRecursively(propertyClass, session, annotateConfig, isFieldReferenced = true)
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
    ) {
      val existingAnnotationFqNames = declaration.annotations.map { it.fqName(session)?.asString()?.toClassId() }

      val newAnnotations = annotateConfig.annotationsToAdd.filter { annotation ->
        val fqName = annotation.toFqName().asString().toClassId()
        if (fqName in existingAnnotationFqNames) {
          if (session.myFirExtensionSessionComponent.enableLogging) {
            println("Skipping duplicate annotation: $fqName for ${declaration.nameAsString}")
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

    override fun check(
      declaration: FirRegularClass,
      context: CheckerContext,
      reporter: DiagnosticReporter,
    ) {
      val component = context.session.myFirExtensionSessionComponent

      if (component.enableLogging) {
        println("ClassChecker: Checking class ${declaration.nameAsString}")
      }

      val annotateConfigs = component.args.annotations.filter { annotate ->
        annotate.annotationsTarget.contains(AnnotationTarget.CLASS) &&
          annotate.packageTarget.any { packageTarget ->
            packageTarget.match(declaration.classId.packageFqName.asString())
          }
      }

      annotateConfigs.forEach { config ->
        annotateClassRecursively(declaration, context.session, config)
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
          val newAnnotations = annotate.annotationsToAdd.flatMap { it.toFirAnnotation(session, declaration) }
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
          val newAnnotations = annotate.annotationsToAdd.flatMap { it.toFirAnnotation(session, declaration) }
          declaration.replaceAnnotations(declaration.annotations + newAnnotations)
        } else {
          if (component.enableLogging) {
            println("PropertyChecker: Conditions not met for annotations on property ${declaration.name}")
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

private fun Annotation.toFirAnnotation(session: FirSession, declaration: FirDeclaration): List<FirAnnotation> {
  return listOf(
    createFirAnnotation(
      fqName = this.toFqName(),
      parameters = parameters,
      declarationName = declaration.nameAsString,
      declarationSource = declaration.source ?: error("Declaration source must not be null"),
      session = session,
    ),
  )
}

private fun Annotation.toFqName(): FqName {
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

  val parametersFinal: Map<String, String> = emptyMap()/*parameters*/
  argumentMapping = parametersFinal.ifEmpty { null }?.let {
    buildAnnotationArgumentMapping {
      it.forEach { (key, value) ->
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
  } ?: FirEmptyAnnotationArgumentMapping

}

public inline fun String.toClassId(): ClassId = FqName(this).run { ClassId(parent(), shortName()) }


private fun isNestedClass(declaration: FirRegularClass, session: FirSession): Boolean {
  val containingClass = declaration.symbol.getContainingClassSymbol(session)
  return containingClass != null
}
