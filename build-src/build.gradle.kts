plugins {
	`embedded-kotlin`
	`kotlin-dsl`
}
repositories {
	mavenCentral()
}
dependencies {
	implementation("com.google.code.gson:gson:2.9.1") // Match loom :)
	implementation(gradleApi())
}
