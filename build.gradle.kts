import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.apache.commons.lang3.SystemUtils
import proguard.gradle.ProGuardTask
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.zip.ZipInputStream

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
val version: String = "$fullVersion-$gitVersion"
val mixinGroup = "$baseGroup.mixin"
project.version = version
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

repositories {
	mavenCentral()
	maven("https://repo.nea.moe/releases/")
	maven("https://repo.spongepowered.org/maven/")
	maven("https://maven.notenoughupdates.org/releases")
	maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

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
	shadowImpl("moe.nea:libautoupdate:1.3.1")
	runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.1.2")
	testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

// Tasks:

// Delete default shadow configuration
tasks.shadowJar {
	doFirst { error("Incorrect shadow JAR built!") }
}

tasks.test {
	useJUnitPlatform()
}

tasks.withType(JavaCompile::class) {
	options.encoding = "UTF-8"
}

abstract class GenerateItemIds : DefaultTask() {
	@get: OutputDirectory
	abstract val outputDirectory: DirectoryProperty

	@get: InputDirectory
	abstract val repoFiles: DirectoryProperty

	@get: Input
	abstract val repoHash: Property<String>

	@get: Input
	abstract val packageName: Property<String>

	@get:Internal
	val outputFile get() = outputDirectory.asFile.get().resolve(packageName.get().replace(".", "/") + "/ItemIds.java")

	@TaskAction
	fun generateItemIds() {
		val nonIdName = "[^A-Z0-9_]".toRegex()

		data class Item(val id: String, val file: File) {
			val javaName get() = id.replace(nonIdName, { "__" + it.value.single().code })
		}

		val items = mutableListOf<Item>()
		for (listFile in repoFiles.asFile.get().resolve("items").listFiles() ?: emptyArray()) {
			listFile ?: continue
			if (listFile.extension != "json") {
				error("Unknown file $listFile")
			}
			items.add(Item(listFile.nameWithoutExtension, listFile))
		}
		items.sortedBy { it.id }
		outputFile.parentFile.mkdirs()
		val writer = outputFile.writer().buffered()
		writer.appendLine("// @generated from " + repoHash.get())
		writer.appendLine("package " + packageName.get() + ";")
		writer.appendLine()
		writer.appendLine("import moe.nea.ledger.ItemId;")
		writer.appendLine()
		writer.appendLine("/**")
		writer.appendLine(" * Automatically generated {@link ItemId} list.")
		writer.appendLine(" */")
		writer.appendLine("@org.jspecify.annotations.NullMarked")
		writer.appendLine("public interface ItemIds {")
		val gson = Gson()
		for (item in items) {
			writer.appendLine("\t/**")
			writer.appendLine("\t * @see <a href=${gson.toJson("https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/blob/${repoHash.get()}/items/${item.id}.json")}>JSON definition</a>")
			writer.appendLine("\t */")
			writer.appendLine("\tItemId ${item.javaName} =" +
					                  " ItemId.forName(${gson.toJson(item.id)});")
		}
		writer.appendLine("}")
		writer.close()
	}
}

abstract class RepoDownload : DefaultTask() {
	@get:Input
	abstract val hash: Property<String>

	@get:OutputDirectory
	abstract val outputDirectory: DirectoryProperty

	init {
		outputDirectory.convention(project.layout.buildDirectory.dir("extracted-test-repo"))
	}

	@TaskAction
	fun performDownload() {
		val outputDir = outputDirectory.asFile.get().absoluteFile
		outputDir.mkdirs()
		URI("https://github.com/notEnoughUpdates/notEnoughUpdates-rEPO/archive/${hash.get()}.zip").toURL().openStream()
			.let(::ZipInputStream)
			.use { zipInput ->
				while (true) {
					val entry = zipInput.nextEntry ?: break
					val destination = outputDir.resolve(
						entry.name.substringAfter('/')).absoluteFile
					require(outputDir in generateSequence(destination) { it.parentFile })
					if (entry.isDirectory) continue
					destination.parentFile.mkdirs()
					destination.outputStream().use { output ->
						zipInput.copyTo(output)
					}
				}
			}
	}
}

val downloadRepo by tasks.register("downloadRepo", RepoDownload::class) {
	hash.set("dcf1dbc")
}

val generateItemIds by tasks.register("generateItemIds", GenerateItemIds::class) {
	repoHash.set(downloadRepo.hash)
	packageName.set("moe.nea.ledger.gen")
	outputDirectory.set(layout.buildDirectory.dir("generated/sources/itemIds"))
	repoFiles.set(downloadRepo.outputDirectory)
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
	println(libJava)
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

configure<BuildConfigExtension> {
	packageName("moe.nea.ledger.gen")
	buildConfigField<String>("VERSION", version)
	buildConfigField<String>("FULL_VERSION", fullVersion)
	buildConfigField<String>("GIT_COMMIT", gitVersion)
}

