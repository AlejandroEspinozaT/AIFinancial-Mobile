pluginManagement {
    repositories {
        // Same reasoning and same scoping as dependencyResolutionManagement
        // below: plugin marker artifacts for com.android.*/androidx.*/Hilt
        // live on Google's Maven, but so do several of the Kotlin Gradle
        // plugin's own transitive dependencies when google() is tried
        // first and unscoped — scoping it here is what lets the
        // kotlin.jvm plugin (used by :core, which must resolve without
        // dl.google.com — see CLAUDE.md) resolve cleanly from Maven
        // Central / the Gradle Plugin Portal instead.
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.google\\.android.*")
                includeGroup("com.google.dagger")
                includeGroup("com.google.gms")
            }
        }
        // Google's own GCS-hosted mirror of Maven Central, tried before
        // mavenCentral() itself. This isn't redundant: Maven Central's
        // Cloudflare front intermittently 404s a plain HEAD request for an
        // artifact that a GET on the exact same URL serves fine (observed
        // directly, not assumed — see CLAUDE.md), and Gradle's dependency
        // resolver relies on HEAD to decide whether an artifact exists at
        // all. This mirror has no such front and resolves the same
        // artifacts reliably through this sandbox's proxy.
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Scoped to the groups Google's Maven actually hosts, so resolving a
        // dependency that lives on Maven Central (e.g. everything :core
        // depends on) never even attempts dl.google.com. This isn't just an
        // optimization here: this sandbox cannot reach dl.google.com at all
        // (see CLAUDE.md), so without this scoping, any lookup for an
        // androidx/com.android/com.google.dagger artifact that tries
        // Google's repo first would hard-fail dependency resolution for the
        // whole build, not just for :app.
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.google\\.android.*")
                includeGroup("com.google.dagger")
            }
        }
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
    }
}

rootProject.name = "AIFinancial-Mobile"
include(":app")
include(":core")
