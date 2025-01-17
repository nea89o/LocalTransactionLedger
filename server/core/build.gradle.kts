plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	application
}


dependencies {
	declareKtorVersion()
	implementation("io.ktor:ktor-server-netty")
	implementation("io.ktor:ktor-server-status-pages")
	implementation("io.ktor:ktor-server-content-negotiation")
	implementation("io.ktor:ktor-serialization-kotlinx-json")
	implementation("io.ktor:ktor-server-compression")
	implementation("sh.ondr:kotlin-json-schema:0.1.1")
	implementation(project(":database:impl"))
	implementation(project(":server:swagger"))

	runtimeOnly("ch.qos.logback:logback-classic:1.5.16")
	runtimeOnly("org.xerial:sqlite-jdbc:3.45.3.0")
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
application {
	val isDevelopment: Boolean = project.ext.has("development")
	applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment",
									   "-Dledger.databasefolder=${project(":mod").file("run/money-ledger").absoluteFile}")
	mainClass.set("moe.nea.ledger.server.core.ApplicationKt")
}
