plugins {
	`java-library`
	kotlin("jvm")
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

dependencies {
	implementation("io.azam.ulidj:ulidj:1.0.4")
}
