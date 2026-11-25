package com.roldanmatias.rickverse.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.parameters
import com.lemonappdev.konsist.api.ext.list.withAnnotationNamed
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Layer rules that the Gradle module graph cannot express (it already forbids illegal *module* dependencies; these
 * forbid illegal *imports and shapes* inside a legal module). Run on the JVM in milliseconds, in CI on every PR.
 */
class ArchitectureTest {
    private val production = Konsist.scopeFromProject().files.filter { "/src/main/" in it.path }
    private val scope = Konsist.scopeFromProduction()

    private fun importsOf(path: String, vararg prefixes: String) =
        production
            .filter { path in it.path }
            .flatMap { file -> file.imports.map { file to it.name } }
            .filter { (_, name) -> prefixes.any { name.startsWith(it) } }
            .map { (file, name) -> "${file.path} imports $name" }

    @Test
    fun `the scan sees the project`() {
        assertTrue("expected many production files, got ${production.size}", production.size > 100)
    }

    @Test
    fun `presentation packages never import Compose or the Android framework`() {
        val violations = production
            .filter { "/presentation/" in it.path }
            .flatMap { file -> file.imports.map { file to it.name } }
            // `androidx.lifecycle` is allowed (ViewModel, SavedStateHandle); `android.` and Compose are not.
            .filter { (_, name) -> name.startsWith("androidx.compose") || name.startsWith("android.") }
            .map { (file, name) -> "${file.path} imports $name" }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun `domain stays pure Kotlin`() {
        val violations = importsOf(
            "/core/domain/src/main/",
            "android.",
            "androidx.",
            "retrofit2",
            "okhttp3",
            "com.apollographql",
            "kotlinx.serialization",
        )
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun `only data-graphql knows Apollo`() {
        val violations = production
            .filter { "/data/graphql/" !in it.path }
            .flatMap { file -> file.imports.map { file to it.name } }
            .filter { (_, name) -> name.startsWith("com.apollographql") }
            .map { (file, name) -> "${file.path} imports $name" }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun `only data-rest knows Retrofit`() {
        val violations = production
            .filter { "/data/rest/" !in it.path && "/core/network/" !in it.path }
            .flatMap { file -> file.imports.map { file to it.name } }
            .filter { (_, name) -> name.startsWith("retrofit2") }
            .map { (file, name) -> "${file.path} imports $name" }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun `features depend on domain abstractions, never on data modules`() {
        val violations = importsOf("/feature/", "com.roldanmatias.rickverse.data.")
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun `hilt view models are named ViewModel and take no Context`() {
        val viewModels = scope.classes().withAnnotationNamed("HiltViewModel")
        assertTrue("no @HiltViewModel classes found", viewModels.isNotEmpty())
        viewModels.assertTrue { it.name.endsWith("ViewModel") }
        viewModels.flatMap { it.primaryConstructor?.parameters.orEmpty() }
            .assertFalse { it.type.name == "Context" || it.type.name == "Application" }
    }

    @Test
    fun `rest DTOs are internal`() {
        val dtos = scope.classes().withPackage("..data.rest.dto..")
        assertTrue("no DTO classes found", dtos.isNotEmpty())
        dtos.assertTrue { it.hasInternalModifier }
    }

    @Test
    fun `use cases live in the usecase package and are named UseCase`() {
        val useCases = scope.classes().withPackage("..core.domain.usecase..")
        assertTrue(useCases.isNotEmpty())
        useCases.assertTrue { it.name.endsWith("UseCase") }
    }
}
