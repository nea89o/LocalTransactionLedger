import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

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

	init {
		repoHash.convention("unknown-repo-git-hash")
	}

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
