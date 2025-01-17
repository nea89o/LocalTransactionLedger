plugins {
	kotlin("jvm")
	application
}

dependencies {
	declareKtorVersion()
	implementation(project(":server:core"))
	implementation(project(":server:frontend"))
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

application {
	mainClass.set("moe.nea.ledger.server.core.ApplicationKt")
}

