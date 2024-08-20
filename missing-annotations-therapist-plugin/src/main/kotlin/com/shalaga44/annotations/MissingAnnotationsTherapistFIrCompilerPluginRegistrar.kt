package com.shalaga44.annotations

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.isAnnotationClass
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.FirAnnotationBuilder
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

val KEY_PACKAGE_TARGETS = CompilerConfigurationKey<List<String>>("fully-qualified packages targets names")
val KEY_ANNOTATIONS = CompilerConfigurationKey<List<String>>("fully-qualified annotations names")


@Suppress("unused")
@AutoService(CompilerPluginRegistrar::class)
class FirMissingAnnotationsTherapistCompilerPluginRegistrar : CompilerPluginRegistrar() {


  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    println("configuration = ${configuration}")
    val annotations = configuration[KEY_ANNOTATIONS]?.map { FqName(it) } ?: emptyList()
    val packageTarget = configuration[KEY_PACKAGE_TARGETS]?.map { FqName(it) } ?: emptyList()
//    if (annotation.isEmpty()) return
    System.err.println("configuration[KEY_ANNOTATIONS] = {${configuration[KEY_ANNOTATIONS]?.joinToString(", ")}}")
    System.err.println("configuration[KEY_PACKAGE_TARGETS] = {${configuration[KEY_PACKAGE_TARGETS]?.joinToString(", ")}}")


    System.err.println("Registering FirMissingAnnotationsTherapistExtensionRegistrar")
    FirExtensionRegistrarAdapter.registerExtension(
      FirMissingAnnotationsTherapistExtensionRegistrar(MissingAnnotationsTherapistArgs(annotations, packageTarget)),
    )
    System.err.println("FirMissingAnnotationsTherapistExtensionRegistrar registered")
  }

  override val supportsK2: Boolean = true
}
data class MissingAnnotationsTherapistArgs(val annotations: List<FqName>, val packageTarget: List<FqName>)
class FirMissingAnnotationsTherapistExtensionRegistrar(val args: MissingAnnotationsTherapistArgs) : FirExtensionRegistrar() {

  override fun ExtensionRegistrarContext.configurePlugin() {
    +FirMissingAnnotationsTherapistExtensionSessionComponent.getFactory(args)
    +::MissingAnnotationsTherapistExtensionFirCheckersExtension
  }

  init {
    System.err.println("FirMissingAnnotationsTherapistExtensionRegistrar initialized")
  }
}



internal class FirMissingAnnotationsTherapistExtensionSessionComponent(
  session: FirSession,
  val args: MissingAnnotationsTherapistArgs,
) : FirExtensionSessionComponent(session) {
  fun isDto(declaration: FirRegularClass): Boolean {
    System.err.println("Evaluating if transformation is needed for: ${declaration::class.simpleName}")

    if (declaration.classKind.isAnnotationClass) {
      System.err.println("Skipping annotation class: ${declaration.classId.asString()}")
      return false
    }

    val isTargetClass = declaration.classId.packageFqName.asString().let{ packageFqName->
      args.packageTarget.any { packageFqName.startsWith(it.asString()) }
    }
    System.err.println("Checked declaration ${declaration.classId.asString()}: needs transformation: $isTargetClass")

    return isTargetClass
  }


  internal companion object {
    internal fun getFactory(args: MissingAnnotationsTherapistArgs): Factory {
      return Factory { session ->
        FirMissingAnnotationsTherapistExtensionSessionComponent(session, args)
      }
    }
  }
}

internal val FirSession.myFirExtensionSessionComponent: FirMissingAnnotationsTherapistExtensionSessionComponent by FirSession.sessionComponentAccessor()


internal class MissingAnnotationsTherapistExtensionFirCheckersExtension(
  session: FirSession,
) : FirAdditionalCheckersExtension(session) {
  override val declarationCheckers: DeclarationCheckers =
    object : DeclarationCheckers() {
      override val regularClassCheckers: Set<FirRegularClassChecker> =
        setOf(MissingAnnotationsTherapistExtensionFirRegularClassChecker)
    }

  internal object MissingAnnotationsTherapistExtensionFirRegularClassChecker :
    FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(
      declaration: FirRegularClass,
      context: CheckerContext,
      reporter: DiagnosticReporter,
    ) {
      val matcher = context.session.myFirExtensionSessionComponent


      if (matcher.isDto(declaration).not()) return

      val errorFactory = when {
        declaration.classKind != ClassKind.CLASS -> Errors.OnNonClass
        declaration.isData -> Errors.OnDataClass
        declaration.hasModifier(KtTokens.VALUE_KEYWORD) -> Errors.OnValueClass
        declaration.isInner -> Errors.OnInnerClass
        declaration.primaryConstructorIfAny(context.session) == null ->
          Errors.PrimaryConstructorRequired

        else -> null
      }
      if (errorFactory != null) {
        reporter.reportOn(
          source = declaration.source,
          factory = errorFactory,
          context = context,
        )
      }
     val newAnnotations = matcher.args.annotations.map { createCustomAnnotation(it) }
      declaration.replaceAnnotations(
        declaration.annotations + newAnnotations,
      )
    }

    private fun createCustomAnnotation(fqName: FqName): FirAnnotation {
      System.err.println("Creating custom annotation")

      val classId = ClassId.topLevel(fqName)

      return createFirAnnotation(
          buildResolvedTypeRef {
              this.type = ConeClassLikeTypeImpl(
                  lookupTag = ConeClassLikeLookupTagImpl(classId),
                  typeArguments = emptyArray<ConeTypeProjection>(),
                  isNullable = false,
              )
          },
      )
    }

  }

  private object Errors : BaseDiagnosticRendererFactory() {

    val OnNonClass by error0<PsiElement>(
      positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
    )

    val OnDataClass by error0<PsiElement>(
      positioningStrategy = SourceElementPositioningStrategies.DATA_MODIFIER,
    )

    val OnValueClass by error0<PsiElement>(
      positioningStrategy = SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER,
    )

    val OnInnerClass by error0<PsiElement>(
      positioningStrategy = SourceElementPositioningStrategies.INNER_MODIFIER,
    )

    val PrimaryConstructorRequired by error0<PsiElement>(
      positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
    )

    val PrimaryConstructorPropertiesRequired by error0<PsiElement>(
      positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
    )

    override val MAP = KtDiagnosticFactoryToRendererMap("MAT").apply {
      put(
        factory = OnNonClass,
        message = "MAT can only be applied to a class",
      )
      put(
        factory = OnDataClass,
        message = "MAT cannot be applied to a data class",
      )
      put(
        factory = OnValueClass,
        message = "MAT cannot be applied to a value class",
      )
      put(
        factory = OnInnerClass,
        message = "MAT cannot be applied to an inner class",
      )
      put(
        factory = PrimaryConstructorRequired,
        message = "MAT class must have a primary constructor",
      )
      put(
        factory = PrimaryConstructorPropertiesRequired,
        message = "MAT class primary constructor must have at least one property",
      )
    }

    init {
      RootDiagnosticRendererFactory.registerFactory(this)
    }
  }

}

public fun createFirAnnotation(
  annotationTypeRef: FirTypeRef,
  argumentMapping: FirAnnotationArgumentMapping = FirEmptyAnnotationArgumentMapping,
  builder: FirAnnotationBuilder.() -> Unit = {},
): FirAnnotation = buildAnnotation {
  this.annotationTypeRef = annotationTypeRef
  this.argumentMapping = argumentMapping
  builder()
}

public fun FirTypeRef.toFirAnnotation(
  argumentMapping: FirAnnotationArgumentMapping = FirEmptyAnnotationArgumentMapping,
  builder: FirAnnotationBuilder.() -> Unit = {},
): FirAnnotation = createFirAnnotation(this, argumentMapping, builder)
