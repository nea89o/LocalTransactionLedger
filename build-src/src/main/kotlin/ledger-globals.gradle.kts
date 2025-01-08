import org.gradle.api.tasks.bundling.AbstractArchiveTask

repositories {
	mavenCentral()
	maven("https://repo.nea.moe/releases/")
	maven("https://repo.spongepowered.org/maven/")
	maven("https://maven.notenoughupdates.org/releases")
	maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

tasks.withType<AbstractArchiveTask> {
	this.isPreserveFileTimestamps = false
	this.isReproducibleFileOrder = true
}

