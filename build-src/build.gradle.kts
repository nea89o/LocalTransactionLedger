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
	api("com.guardsquare:proguard-gradle:7.6.1")
}
