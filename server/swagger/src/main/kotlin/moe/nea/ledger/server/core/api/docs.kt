package moe.nea.ledger.server.core.api

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.response.respond
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.PathSegmentConstantRouteSelector
import io.ktor.server.routing.RootRouteSelector
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.TrailingSlashRouteSelector
import io.ktor.server.routing.get
import io.ktor.util.AttributeKey
import sh.ondr.jsonschema.JsonSchema
import sh.ondr.jsonschema.jsonSchema


fun Route.openApiDocsJson() {
	get {
		val docs = plugin(Documentation)
		val model = docs.finalizeJson()
		call.respond(model)
	}
}

class DocumentationPath(val path: String) {

	companion object {
		fun createDocumentationPath(baseRoute: Route?, route: Route): DocumentationPath {
			return DocumentationPath(createRoutePath(baseRoute, route as RoutingNode))
		}

		private fun createRoutePath(
			baseRoute: Route?,
			route: RoutingNode,
		): String {
			if (baseRoute == route)
				return "/"
			val parent = route.parent
			if (parent == null) {
				if (baseRoute != null)
					error("Could not find $route in $baseRoute")
				return "/"
			}
			var parentPath = createRoutePath(baseRoute, parent)
			if (!parentPath.endsWith("/"))
				parentPath += "/"
			return when (val selector = route.selector) {
				is TrailingSlashRouteSelector -> parentPath
				is RootRouteSelector -> parentPath
				is PathSegmentConstantRouteSelector -> parentPath + selector.value
				is HttpMethodRouteSelector -> parentPath // TODO: generate a separate path here
				else -> error("Could not comprehend $selector (${selector.javaClass})")
			}
		}
	}
}

class Response {
	var schema: JsonSchema? = null
	inline fun <reified T : Any> schema() {
		schema = jsonSchema<T>()
	}

	fun intoJson(): OpenApiResponse {
		return OpenApiResponse(
			"",
			mapOf(
				ContentType.Application.Json to OpenApiResponseContentType(schema)
			)
		)
	}
}

class DocumentationContext(val path: DocumentationPath) {
	val responses = mutableMapOf<HttpStatusCode, Response>()
	fun responds(statusCode: HttpStatusCode, block: Response.() -> Unit) {
		responses.getOrPut(statusCode) { Response() }.also(block)
	}

	fun respondsOk(block: Response.() -> Unit) {
		responds(HttpStatusCode.OK, block)
	}

	var summary: String = ""
	var description: String = ""
	fun intoJson(): OpenApiRoute {
		return OpenApiRoute(
			summary,
			description,
			get = OpenApiOperation(
				tags = listOf(), // TODO: tags
				summary = "",
				description = "",
				operationId = "",
				deprecated = false,
				responses = responses.mapValues {
					it.value.intoJson()
				}
			),
			post = null, patch = null, delete = null, // TODO: generate separate contexts for those
		)
	}
}


class Documentation(config: Configuration) {
	companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, Documentation> {
		override val key: AttributeKey<Documentation> = AttributeKey("LedgerDocumentation")

		override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Documentation {
			val config = Configuration().also(configure)
			val plugin = Documentation(config)
			return plugin
		}
	}

	val info = config.info
	var root: RoutingNode? = null
	private val documentationNodes = mutableMapOf<DocumentationPath, DocumentationContext>()
	fun createDocumentationNode(path: DocumentationPath) =
		documentationNodes.getOrPut(path) { DocumentationContext(path) }

	private val openApiJson by lazy {
		OpenApiModel(
			info = info,
			// TODO: generate server list better
			servers = listOf(Server("http://localhost:8080", "Local Server")),
			paths = documentationNodes.map {
				OpenApiPath(it.key.path) to it.value.intoJson()
			}.toMap()
		)
	}

	fun finalizeJson(): OpenApiModel {
		return openApiJson
	}

	class Configuration {
		var info: Info = Info(
			title = "Example API Docs",
			description = "Missing description",
			version = "0.0.0"
		)
	}
}

fun Route.docs(block: DocumentationContext.() -> Unit) {
	val documentation = plugin(Documentation)
	val documentationPath = DocumentationPath.createDocumentationPath(documentation.root, this)
	val node = documentation.createDocumentationNode(documentationPath)
	block(node)
}

