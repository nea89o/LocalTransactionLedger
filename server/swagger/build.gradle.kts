plugins {
	`java-library`
	kotlin("jvm")
	kotlin("plugin.serialization")
}


dependencies {
	declareKtorVersion()
	api("io.ktor:ktor-server-core")
	api("sh.ondr:kotlin-json-schema:0.1.1")
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
