plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	application
	id("com.github.gmazzo.buildconfig")
}


dependencies {
	declareKtorVersion()
	api("io.ktor:ktor-server-netty")
	api("io.ktor:ktor-server-status-pages")
	api("io.ktor:ktor-server-content-negotiation")
	api("io.ktor:ktor-serialization-kotlinx-json")
	api("io.ktor:ktor-server-compression")
	api("io.ktor:ktor-server-cors")
	api("sh.ondr:kotlin-json-schema:0.1.1")
	api(project(":server:analysis"))
	api(project(":database:impl"))
	api(project(":server:swagger"))

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
buildConfig {
	packageName("moe.nea.ledger.gen")
}
