@file:OptIn(
    ExperimentalKotlinGradlePluginApi::class,
    ExperimentalAbiValidation::class,
    ExperimentalWasmDsl::class
)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import kotlin.jvm.optionals.getOrNull

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    val versionCatalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

    println(versionCatalog::class)

    explicitApi()
    abiValidation()

    compilerOptions {
        allWarningsAsErrors = true
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xreturn-value-checker=full"
        )
    }

    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(versionCatalog.getVersion("java"))
    }

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
            //jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        }
    }

    android {
        compileSdk = versionCatalog.getVersion("android").toInt()

        minSdk {
            minSdk = 26
        }

        withHostTest {}
    }

    js {
        outputModuleName = project.name
        browser {
            testTask {
                useKarma {
                    useChromiumHeadless()
                }
            }
        }
    }

    wasmJs {
        outputModuleName = "${project.name}Wasm"
        nodejs()
        browser {
            testTask {
                useKarma {
                    useChromiumHeadless()
                }
            }
        }
    }

    wasmWasi {
        nodejs()
    }

    nativeTargets()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    applyDefaultHierarchyTemplate {
        common {
            group("native") {
                group("unix") {
                    group("linux")
                    group("apple")
                    group("androidNative")
                }
            }
            group("wasm") {
                withWasmJs()
                withWasmWasi()
            }
        }
    }
}

/** See [Kotlin/Native supported targets and hosts](https://kotlinlang.org/docs/native-target-support.html) */
fun KotlinMultiplatformExtension.nativeTargets() {
    // Tier 1
    macosArm64()
    iosSimulatorArm64()
    iosArm64()

    // Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosArm64()

    // Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    watchosDeviceArm64()
    iosX64()

    @Suppress("DEPRECATION")
    run {
        macosX64()
        watchosX64()
        tvosX64()
    }
}

fun VersionCatalog.getVersion(alias: String): String {
    return findVersion(alias).getOrNull()?.requiredVersion
        ?: throw GradleException("No '$alias' version specified in the version catalog")
}

tasks.register("printKotlinSourceSetHierarchy") {
    doLast {
        val sourceSets = kotlin.sourceSets.toList()

        val childrenByParent = sourceSets
            .flatMap { child ->
                child.dependsOn.map { parent -> parent to child }
            }
            .groupBy(keySelector = { it.first }) { it.second }

        val roots = sourceSets
            .filter { it.dependsOn.isEmpty() }
            .sortedBy { it.name }

        println("Found ${roots.size} roots")

        fun printTree(
            sourceSet: KotlinSourceSet,
            prefix: String = "",
            isRoot: Boolean = false,
            isLast: Boolean = true
        ) {
            val connector = when {
                isRoot -> ""
                isLast -> " └─ "
                else -> " ├─ "
            }

            println("$prefix$connector${sourceSet.name}")

            val children = childrenByParent[sourceSet]
                .orEmpty()
                .sortedBy { it.name }

            val childPrefix = when {
                isRoot -> prefix
                isLast -> "$prefix    "
                else -> "$prefix │  "
            }

            children.forEachIndexed { i, child ->
                printTree(child, childPrefix, isLast = i == children.lastIndex)
            }
        }

        roots.forEachIndexed { i, root ->
            printTree(root, isRoot = true)

            if (i != roots.lastIndex) {
                println()
            }
        }
    }
}
