// Pure-Kotlin/JVM module — no AndroidX, no Android SDK dependency, on purpose.
// See specs/architecture/technology-choices.md: this is the one module whose
// tests can actually run in a sandbox without access to Google's Maven repo.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // No main dependencies yet — this module's current code (queue state
    // machine, pairing result) is plain Kotlin with no coroutine usage. Add
    // kotlinx-coroutines-core here only once something in :core actually
    // needs it (a suspend function or Flow), not speculatively ahead of need.

    // Plain JUnit4 assertions only, deliberately not Truth/MockK here: both
    // pull in a large transitive tree (Guava, AutoValue, ErrorProne, ASM)
    // that made this module's resolution unreliable against Maven Central
    // through this sandbox's proxy (observed rate-limiting on the long tail
    // of transitive artifacts). :app's instrumented tests (built/run only in
    // Android Studio, not here) can still use Truth/MockK per
    // specs/architecture/technology-choices.md — this is a sandbox-reliability
    // choice specific to what must resolve here, not a project-wide rule.
    testImplementation(libs.junit)
}

// No explicit jvmToolchain: that asks Gradle to locate/provision a separate
// JDK matching the requested version, which needs a toolchain download
// repository this sandbox can't reach either. The Kotlin compiler can target
// JVM 17 bytecode from the JDK Gradle itself is already running on (21 here)
// without a distinct toolchain — see CLAUDE.md for the related Google-Maven
// constraint this project already works around the same way.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
