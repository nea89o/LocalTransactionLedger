import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.commons.lang3.SystemUtils
import proguard.gradle.ProGuardTask
import java.io.ByteArrayOutputStream

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath("com.guardsquare:proguard-gradle:7.6.1")
	}
}

plugins {
	idea
	java
	id("gg.essential.loom") version "0.10.0.+"
	id("dev.architectury.architectury-pack200") version "0.1.3"
	id("com.github.johnrengelman.shadow") version "8.1.1"
	id("com.github.gmazzo.buildconfig") version "5.5.0"
	kotlin("jvm") version "2.0.20"
	id("ledger-marker")
	id("ledger-repo")
}

fun cmd(vararg args: String): String {
	val baos = ByteArrayOutputStream()
	exec {
		standardOutput = baos
		commandLine(*args)
	}
	return baos.toByteArray().decodeToString().trim()
}


val baseGroup: String by project
val mcVersion: String by project
val gitVersion = cmd("git", "rev-parse", "--short", "HEAD")
val fullVersion = project.property("mod_version").toString()
val versionName: String = "$fullVersion-$gitVersion"
val mixinGroup = "$baseGroup.mixin"
allprojects {
	version = versionName
}
val modid: String by project

// Toolchains:
java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

// Minecraft configuration:
loom {
	log4jConfigs.from(file("log4j2.xml"))
	launchConfigs {
		"client" {
			property("mixin.debug", "true")
			arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
			arg("--tweakClass", "io.github.notenoughupdates.moulconfig.tweaker.DevelopmentResourceTweaker")
		}
	}
	runConfigs {
		"client" {
			if (SystemUtils.IS_OS_MAC_OSX) {
				// This argument causes a crash on macOS
				vmArgs.remove("-XstartOnFirstThread")
			}
		}
		remove(getByName("server"))
	}
	forge {
		pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
		mixinConfig("mixins.$modid.json")
	}
	mixin {
		defaultRefmapName.set("mixins.$modid.refmap.json")
	}
}

tasks.compileJava {
	dependsOn(tasks.processResources)
}

sourceSets.main {
	output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
	java.srcDir(layout.projectDirectory.dir("src/main/kotlin"))
	kotlin.destinationDirectory.set(java.destinationDirectory)
}

allprojects {
	repositories {
		mavenCentral()
		maven("https://repo.nea.moe/releases/")
		maven("https://repo.spongepowered.org/maven/")
		maven("https://maven.notenoughupdates.org/releases")
		maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
	}
}

// TODO: Add an extra shadow configuration for optimizable jars
//val optShadowImpl: Configuration by configurations.creating {
//
//}

val shadowImpl: Configuration by configurations.creating {
	configurations.implementation.get().extendsFrom(this)
}

dependencies {
	minecraft("com.mojang:minecraft:1.8.9")
	mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
	forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

	shadowImpl(kotlin("stdlib-jdk8"))
	implementation("org.jspecify:jspecify:1.0.0")

	shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
		isTransitive = false
	}
	annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")

	shadowImpl("org.xerial:sqlite-jdbc:3.45.3.0")
	shadowImpl("org.notenoughupdates.moulconfig:legacy:3.0.0-beta.9")
	shadowImpl("io.azam.ulidj:ulidj:1.0.4")
	shadowImpl(project(":dependency-injection"))
	shadowImpl(project(":database:core"))
	shadowImpl("moe.nea:libautoupdate:1.3.1")
	runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
	testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

// Tasks:

// Delete default shadow configuration
tasks.shadowJar {
	doFirst { error("Incorrect shadow JAR built!") }
}

allprojects {
	tasks.withType<Test> {
		useJUnitPlatform()
	}

	tasks.withType(JavaCompile::class) {
		options.encoding = "UTF-8"
	}
}

tasks.downloadRepo {
	hash.set("dcf1dbc")
}

val generateItemIds by tasks.register("generateItemIds", GenerateItemIds::class) {
	repoHash.set(tasks.downloadRepo.get().hash)
	packageName.set("moe.nea.ledger.gen")
	outputDirectory.set(layout.buildDirectory.dir("generated/sources/itemIds"))
	repoFiles.set(tasks.downloadRepo.get().outputDirectory)
}
sourceSets.main {
	java.srcDir(generateItemIds)
}

tasks.withType(Jar::class) {
	archiveBaseName.set(modid)
	manifest.attributes.run {
		this["FMLCorePluginContainsFMLMod"] = "true"
		this["ForceLoadAsMod"] = "true"

		// If you don't want mixins, remove these lines
		this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
		this["MixinConfigs"] = "mixins.$modid.json"
	}
}

tasks.processResources {
	inputs.property("version", project.version)
	inputs.property("mcversion", mcVersion)
	inputs.property("modid", modid)
	inputs.property("basePackage", baseGroup)

	filesMatching(listOf("mcmod.info", "mixins.$modid.json")) {
		expand(inputs.properties)
	}

	rename("(.+_at.cfg)", "META-INF/$1")
}


val proguardOutJar = project.layout.buildDirectory.file("badjars/stripped.jar")
val proguard = tasks.register("proguard", ProGuardTask::class) {
	dependsOn(tasks.jar)
	injars(tasks.jar.map { it.archiveFile })
	outjars(proguardOutJar)
	configuration(file("ledger-rules.pro"))
	verbose()
	val libJava = javaToolchains.launcherFor(java.toolchain)
		.get()
		.metadata.installationPath.file("jre/lib/rt.jar")
	libraryjars(libJava)
	libraryjars(configurations.compileClasspath)
}

val shadowJar2 = tasks.register("shadowJar2", ShadowJar::class) {
	destinationDirectory.set(layout.buildDirectory.dir("badjars"))
	archiveClassifier.set("all-dev")
	from(proguardOutJar)
	dependsOn(proguard)
	configurations = listOf(shadowImpl)
	relocate("moe.nea.libautoupdate", "moe.nea.ledger.deps.libautoupdate")
	mergeServiceFiles()
	exclude(
		"META-INF/INDEX.LIST",
		"META-INF/*.SF",
		"META-INF/*.DSA",
		"META-INF/*.RSA",
		"module-info.class",
	)
}
val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
	archiveClassifier.set("")
	from(shadowJar2)
	input.set(shadowJar2.get().archiveFile)
}

tasks.jar {
	archiveClassifier.set("without-deps")
	destinationDirectory.set(layout.buildDirectory.dir("badjars"))
}


tasks.assemble.get().dependsOn(tasks.remapJar)

inline fun <reified T : Any> ExtensionAware.configureIf(crossinline block: T.() -> Unit) {
	if (extensions.findByType<T>() != null) {
		extensions.configure<T> { block() }
	}
}

allprojects {
	configureIf<BuildConfigExtension> {
		packageName("moe.nea.ledger.gen")
		buildConfigField<String>("VERSION", versionName)
		buildConfigField<String>("FULL_VERSION", fullVersion)
		buildConfigField<String>("GIT_COMMIT", gitVersion)
	}
}

