import org.gradle.api.tasks.bundling.AbstractArchiveTask

tasks.withType<AbstractArchiveTask> {
	this.isPreserveFileTimestamps = false
	this.isReproducibleFileOrder = true
}