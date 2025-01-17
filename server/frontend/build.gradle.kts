import com.github.gradle.node.pnpm.task.PnpmTask

plugins {
	id("com.github.node-gradle.node") version "7.1.0"
	`java-library`
}

val webDist by tasks.register("webDist", PnpmTask::class) {
	args.addAll("build")
	outputs.dir("dist")
}
tasks.jar {
	from(webDist) {
		into("ledger-web-dist/")
	}
}
