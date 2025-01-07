plugins {
	kotlin("jvm") version "2.0.20"
	`kotlin-dsl`
}
repositories {
	mavenCentral()
}
dependencies {
	implementation("com.google.code.gson:gson:2.11.0")
	implementation(gradleApi())
}
