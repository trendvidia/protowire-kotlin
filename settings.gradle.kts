// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "protowire-kotlin"

// Composite build: pull in the Java port's modules from the sibling
// repo. Until the Java artifacts are published to Maven Central
// (ROADMAP 0.72.0), this is how dependent code resolves them.
//
// Required prerequisite: the protowire-java repo must be checked out
// at ../protowire-java/. Without it, every dependency below fails to
// resolve. Same constraint cmd/protowire CLI builds put on
// ../protowire-go/.
includeBuild("../protowire-java")
