import com.github.gmazzo.buildconfig.BuildConfigExtension
import java.io.ByteArrayOutputStream

plugins {
	val kotlinVersion = "2.0.20"
	kotlin("jvm") version kotlinVersion apply false
	kotlin("plugin.serialization") version kotlinVersion apply false
	id("com.github.gmazzo.buildconfig") version "5.5.0" apply false
	id("ledger-globals")
	id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

allprojects {
	apply(plugin = "ledger-globals")
}

fun cmd(vararg args: String): String {
	val baos = ByteArrayOutputStream()
	exec {
		standardOutput = baos
		commandLine(*args)
	}
	return baos.toByteArray().decodeToString().trim()
}

val gitVersion = cmd("git", "rev-parse", "--short", "HEAD")
val fullVersion = project.property("mod_version").toString()
val versionName: String = "$fullVersion-$gitVersion"
allprojects {
	version = versionName
	afterEvaluate {
		configureIf<BuildConfigExtension> {
			buildConfigField<String>("VERSION", versionName)
			buildConfigField<String>("FULL_VERSION", fullVersion)
			buildConfigField<String>("GIT_COMMIT", gitVersion)
		}
	}
}
