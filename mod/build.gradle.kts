import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import proguard.gradle.ProGuardTask

plugins {
	idea
	java
	id("gg.essential.loom") version "1.6.+"
	id("dev.architectury.architectury-pack200") version "0.1.3"
	id("com.github.johnrengelman.shadow") version "8.1.1"
	id("com.github.gmazzo.buildconfig")
	kotlin("jvm")
	id("ledger-repo")
}
val baseGroup: String by project
val mcVersion: String by project
val mixinGroup = "$baseGroup.mixin"
val modid: String by project

// Toolchains:
java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

// Minecraft configuration:
loom {
	forge {
		pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
		mixinConfig("mixins.$modid.json")
	}
	log4jConfigs.from(file("log4j2.xml"))
	runConfigs {
		"client" {
			property("ledger.bonusresourcemod", sourceSets.main.get().output.resourcesDir!!.absolutePath)
			property("mixin.debug", "true")
			programArgs("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
			programArgs("--tweakClass", "io.github.notenoughupdates.moulconfig.tweaker.DevelopmentResourceTweaker")
		}
		remove(getByName("server"))
	}
	mixin.useLegacyMixinAp.set(false)
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

	shadowImpl("org.xerial:sqlite-jdbc:3.45.3.0")
	shadowImpl("org.notenoughupdates.moulconfig:legacy:3.0.0-beta.9")
	shadowImpl("io.azam.ulidj:ulidj:1.0.4")
	shadowImpl(project(":dependency-injection"))
	shadowImpl(project(":database:core"))
	shadowImpl("moe.nea:libautoupdate:1.3.1") {
		exclude(module = "gson")
	}
	runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
	testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

// Tasks:

// Delete default shadow configuration
tasks.shadowJar {
	doFirst { error("Incorrect shadow JAR built!") }
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
tasks.withType<AbstractArchiveTask> {
	archiveBaseName.set(modid)
}
tasks.withType<Jar> {
	manifest.attributes.run {
		this["FMLCorePluginContainsFMLMod"] = "true"
		this["ForceLoadAsMod"] = "true"
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
}


val proguardOutJar = project.layout.buildDirectory.file("badjars/stripped.jar")
val proguard = tasks.register("proguard", ProGuardTask::class) {
	dependsOn(tasks.jar)
	injars(tasks.jar.map { it.archiveFile })
	outjars(proguardOutJar)
	configuration(file("ledger-rules.pro"))
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
	relocate("io.github.notenoughupdates.moulconfig", "moe.nea.ledger.deps.moulconfig")
	relocate("io.azam.ulidj", "moe.nea.ledger.deps.ulid")
	mergeServiceFiles()
	exclude(
		// Signatures
		"META-INF/INDEX.LIST",
		"META-INF/*.SF",
		"META-INF/*.DSA",
		"META-INF/*.RSA",
		"module-info.class",

		"META-INF/*.kotlin_module",
		"META-INF/versions/**"
	)
}
tasks.remapJar {
	archiveClassifier.set("")
	inputFile.set(shadowJar2.flatMap { it.archiveFile })
}

tasks.jar {
	archiveClassifier.set("without-deps")
	destinationDirectory.set(layout.buildDirectory.dir("badjars"))
}

tasks.assemble.get().dependsOn(tasks.remapJar)

buildConfig {
	packageName("moe.nea.ledger.gen")
}

