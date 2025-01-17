pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net")
		maven("https://jitpack.io")
		mavenCentral()
		google()
		mavenCentral()
		gradlePluginPortal()
		maven("https://oss.sonatype.org/content/repositories/snapshots")
		maven("https://maven.architectury.dev/")
		maven("https://maven.minecraftforge.net/")
		maven("https://repo.spongepowered.org/maven/")
		maven("https://repo.sk1er.club/repository/maven-releases/")
	}
	resolutionStrategy {
		eachPlugin {
			when (requested.id.id) {
				"gg.essential.loom" -> useModule("gg.essential:architectury-loom:${requested.version}")
			}
		}
	}}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}


rootProject.name = "ledger"
include("dependency-injection")
include("database:core")
include("database:impl")
include("basetypes")
include("mod")
include("server:swagger")
include("server:core")
include("server:frontend")
include("server:aio")
includeBuild("build-src")
