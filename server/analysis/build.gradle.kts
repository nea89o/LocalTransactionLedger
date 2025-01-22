plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("com.google.devtools.ksp")
}

dependencies {
	api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")
	ksp("dev.zacsweers.autoservice:auto-service-ksp:1.2.0")
	implementation("com.google.auto.service:auto-service-annotations:1.1.1")
	implementation(project(":database:impl"))
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
