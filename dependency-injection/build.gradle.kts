plugins {
	`java-library`
	kotlin("jvm")
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}
