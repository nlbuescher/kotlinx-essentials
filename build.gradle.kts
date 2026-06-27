@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("multiplatform-conventions")
    id("dokka-conventions")
}

group = "dev.buescher.kotlinx.essentials"
version = "0.1.0-SNAPSHOT"

kotlin {
    android {
        namespace = "dev.buescher.kotlinx.essentials"
    }
}
