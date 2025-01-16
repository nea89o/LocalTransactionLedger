plugins {
	`java-library`
	kotlin("jvm")
}

dependencies {
	api(project(":database:core"))
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}
