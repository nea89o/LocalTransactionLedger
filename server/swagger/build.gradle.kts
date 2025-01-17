plugins {
	`java-library`
	kotlin("jvm")
	kotlin("plugin.serialization")
}


dependencies {
	declareKtorVersion()
	api("io.ktor:ktor-server-core")
	api("io.ktor:ktor-server-webjars")
	api("sh.ondr:kotlin-json-schema:0.1.1")
	implementation("org.webjars:swagger-ui:5.18.2")
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
