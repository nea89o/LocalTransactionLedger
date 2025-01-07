plugins {
	`java-library`
	kotlin("jvm")
}

dependencies {
	api(project(":basetypes"))
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}
