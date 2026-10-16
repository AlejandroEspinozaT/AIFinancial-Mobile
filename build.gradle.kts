// Intentionally empty: each module (:app, :core) applies its own plugins
// directly via the version catalog, since each plugin here only has a
// single consumer module — there's no "apply once at root, use in many
// modules" version-alignment problem to solve, so there's no reason to
// force Gradle to resolve :app's plugins (com.android.application,
// com.google.dagger.hilt.android — both only on Google's Maven) just to
// configure the root project. That would otherwise break running any task
// scoped to :core alone in an environment that cannot reach Google's Maven
// (see CLAUDE.md) — see also gradle.properties' org.gradle.configureondemand.
