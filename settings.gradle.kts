// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "protowire-kotlin"

// The Java port comes from Maven Central. To build against an unreleased
// checkout, pass -PprotowireJavaDir=../protowire-java. Java publishes its
// modules as protowire-<module>, which Gradle cannot match to the project
// names on its own, so each one is mapped here.
providers.gradleProperty("protowireJavaDir").orNull?.let { javaDir ->
    includeBuild(javaDir) {
        dependencySubstitution {
            for (module in listOf("pxf", "pb", "sbe", "envelope")) {
                substitute(module("org.protowire:protowire-$module")).using(project(":$module"))
            }
        }
    }
}
