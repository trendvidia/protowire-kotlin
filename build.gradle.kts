// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.

import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import java.util.zip.ZipFile

plugins {
    // vanniktech 0.36.0 requires Kotlin plugin >= 2.2.0; pin to a
    // current stable release. Drop-in compatible with the existing
    // sources (no language-mode 2 features used).
    kotlin("jvm") version "2.4.21"
    `java-library`
    id("com.vanniktech.maven.publish") version "0.37.0"
    // KDoc for the javadoc jar: javadoc itself does not read Kotlin (#32).
    id("org.jetbrains.dokka") version "2.3.0"
}

group = "org.protowire"
version = "2.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Java port codecs, from Maven Central. Java publishes each module as
    // protowire-<module>. Pulls in protobuf-java transitively; consumers
    // who want the lite tier should depend on the *-android Java modules
    // directly (a future :*-kotlin-android variant could mirror this
    // module's structure when there's demand).
    api("org.protowire:protowire-pxf:2.0.1")
    api("org.protowire:protowire-pb:2.0.1")
    api("org.protowire:protowire-sbe:2.0.1")
    api("org.protowire:protowire-envelope:2.0.1")

    // Coroutines for the suspend wrappers + Flow extensions. api because
    // Flow is in this module's public signatures.
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// The javadoc jar must document the public API (#32). `javadoc` reads no
// Kotlin, so the jar shipped with only a manifest until Dokka built it.
// `build` runs `check`, in CI and before every publish, so this gates both.
val checkJavadocJar = tasks.register("checkJavadocJar") {
    description = "Fails unless the javadoc jar has a page for each public declaration."
    group = "verification"
    // The javadoc artifact the Maven publication uploads, however it is built.
    val published = provider {
        publishing.publications.withType<MavenPublication>().single()
            .artifacts.single { it.classifier == "javadoc" }
    }
    dependsOn(published.map { it.buildDependencies })
    val sources = fileTree("src/main/kotlin")
    inputs.files(sources)
    doLast {
        val pages = ZipFile(published.get().file).use { zip ->
            zip.entries().asSequence().associate { e -> e.name to zip.getInputStream(e).readBytes().decodeToString() }
        }
        // A top-level `public` fun or class, past type parameters and a receiver.
        val decl = Regex("""^public (?:(?:suspend|inline|sealed|data|annotation) )*(fun|class) (?:<[^>]+> )?(?:[\w<>?, ]+\.)?(\w+)""")
        // Dokka's file names: each capital letter becomes '-' and lowercase.
        fun kebab(name: String) = name.replace(Regex("[A-Z]")) { "-" + it.value.lowercase() }
        val dir = "protowire-kotlin/org.protowire.kotlin/"
        val expected = sources.files.flatMap { it.readLines() }.mapNotNull { decl.find(it) }.map { m ->
            val (kind, name) = m.destructured
            dir + kebab(name) + if (kind == "class") "/index.html" else ".html"
        }.toSet()
        check(expected.size >= 20) { "found only ${expected.size} public declarations; the pattern no longer reads the sources" }
        val missing = expected - pages.keys
        check(missing.isEmpty()) { "javadoc jar has no page for: $missing" }
        // pbMarshal's KDoc, so the pages carry doc comments and not just signatures.
        val kdoc = "Suspending PB struct marshal"
        check(pages.values.any { kdoc in it }) { "javadoc jar holds no KDoc text (looked for \"$kdoc\")" }
        logger.lifecycle("${published.get().file.name}: ${pages.size} entries, a page for each of ${expected.size} public declarations")
    }
}
tasks.named("check") { dependsOn(checkJavadocJar) }

// --- Maven Central publishing ---------------------------------------------
mavenPublishing {
    // Central Portal is the only supported Sonatype host since
    // vanniktech 0.34.0 removed `SonatypeHost` (OSSRH was retired
    // upstream), so no host argument is passed any more.
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates("org.protowire", "protowire-kotlin", project.version.toString())

    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = SourcesJar.Sources(),
        ),
    )

    pom {
        name.set("protowire-kotlin")
        description.set(
            "Kotlin extensions companion for protowire-java — suspend wrappers, " +
                "DSL builders, and Flow extensions over the JVM codec family."
        )
        url.set("https://protowire.org")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("trendvidia")
                name.set("TrendVidia, LLC.")
                url.set("https://trendvidia.com")
            }
        }

        scm {
            url.set("https://github.com/trendvidia/protowire-kotlin")
            connection.set("scm:git:https://github.com/trendvidia/protowire-kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/trendvidia/protowire-kotlin.git")
        }

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/trendvidia/protowire-kotlin/issues")
        }
    }
}
