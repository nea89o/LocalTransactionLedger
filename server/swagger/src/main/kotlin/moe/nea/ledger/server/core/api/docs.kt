package moe.nea.ledger.server.core.api

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.defaultForFilePath
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.host
import io.ktor.server.application.port
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.PathSegmentConstantRouteSelector
import io.ktor.server.routing.RootRouteSelector
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.TrailingSlashRouteSelector
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.util.AttributeKey
import io.ktor.util.cio.KtorDefaultPool
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.serialization.json.JsonPrimitive
import sh.ondr.jsonschema.JsonSchema
import sh.ondr.jsonschema.jsonSchema
import java.io.File
import java.io.InputStream


fun Route.openApiDocsJson() {
	get {
		val docs = plugin(Documentation)
		val model = docs.finalizeJson()
		call.respond(model)
	}
}

fun Route.openApiUi(apiJsonUrl: String) {
	get("swagger-initializer.js") {
		call.respondText(
			//language=JavaScript
			"""
			window.onload = function() {
			  //<editor-fold desc="Changeable Configuration Block">

			  // the following lines will be replaced by docker/configurator, when it runs in a docker-container
			  window.ui = SwaggerUIBundle({
			    url: ${JsonPrimitive(apiJsonUrl)},
			    dom_id: '#swagger-ui',
			    deepLinking: true,
			    presets: [
			      SwaggerUIBundle.presets.apis,
			      SwaggerUIStandalonePreset
			    ],
			    plugins: [
			      SwaggerUIBundle.plugins.DownloadUrl
			    ],
			    layout: "StandaloneLayout"
			  });

			  //</editor-fold>
			};
		""".trimIndent())
	}
//	val swaggerUiProperties =
//		environment.classLoader.getResource("/META-INF/maven/org.webjars/swagger-ui/pom.properties")
//			?: error("Could not find swagger webjar")
//	val swaggerUiZip = swaggerUiProperties.toString().substringBefore("!")
	val pathParameterName = "static-content-path-parameter"
	route("{$pathParameterName...}") {
		get {
			var requestedPath = call.parameters.getAll(pathParameterName)?.joinToString(File.separator) ?: ""
			requestedPath = requestedPath.replace("\\", "/")
			if (requestedPath.isEmpty()) requestedPath = "index.html"
			if (requestedPath.contains("..")) {
				call.respondText("Forbidden", status = HttpStatusCode.Forbidden)
				return@get
			}
			//TODO: I mean i should read out the version properties but idc
			val version = "5.18.2"
			val resource =
				environment.classLoader.getResourceAsStream("META-INF/resources/webjars/swagger-ui/$version/$requestedPath")

			if (resource == null) {
				call.respondText("Not Found", status = HttpStatusCode.NotFound)
				return@get
			}

			call.respond(InputStreamContent(resource, ContentType.defaultForFilePath(requestedPath)))
		}
	}
}

internal class InputStreamContent(
	private val input: InputStream,
	override val contentType: ContentType
) : OutgoingContent.ReadChannelContent() {

	override fun readFrom(): ByteReadChannel = input.toByteReadChannel(pool = KtorDefaultPool)
}

class DocumentationPath(val path: String)

class DocumentationEndpoint private constructor() {
	var method: HttpMethod = HttpMethod.Get
		private set
	lateinit var path: DocumentationPath
		private set

	private fun initFromPath(
		baseRoute: Route?,
		route: RoutingNode,
	) {
		path = DocumentationPath(createRoutePath(baseRoute, route))
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
		val parentPath = createRoutePath(baseRoute, parent)
		var parentPathAppendable = parentPath
		if (!parentPathAppendable.endsWith("/"))
			parentPathAppendable += "/"
		return when (val selector = route.selector) {
			is TrailingSlashRouteSelector -> parentPathAppendable
			is RootRouteSelector -> parentPath
			is PathSegmentConstantRouteSelector -> parentPathAppendable + selector.value
			is HttpMethodRouteSelector -> {
				method = selector.method
				parentPath
			}

			else -> error("Could not comprehend $selector (${selector.javaClass})")
		}
	}

	companion object {
		fun createDocumentationPath(baseRoute: Route?, route: Route): DocumentationEndpoint {
			val path = DocumentationEndpoint()
			path.initFromPath(baseRoute, route as RoutingNode)
			return path
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

interface IntoTag {
	fun intoTag(): String
}

class DocumentationOperationContext(val route: DocumentationContext) {
	val responses = mutableMapOf<HttpStatusCode, Response>()
	fun responds(statusCode: HttpStatusCode, block: Response.() -> Unit) {
		responses.getOrPut(statusCode) { Response() }.also(block)
	}

	fun respondsOk(block: Response.() -> Unit) {
		responds(HttpStatusCode.OK, block)
	}

	var summary: String = ""
	var description: String = ""
	var deprecated: Boolean = false
	var operationId: String = ""
	val tags: MutableList<String> = mutableListOf()
	val parameters: MutableList<OpenApiParameter> = mutableListOf()
	fun tag(vararg tag: String) {
		tags.addAll(tag)
	}

	fun tag(vararg tag: IntoTag) {
		tag.mapTo(tags) { it.intoTag() }
	}

	inline fun <reified T : Any> queryParameter(name: String, description: String = "") {
		parameter(ParameterLocation.QUERY, name, description, jsonSchema<T>())
	}

	fun parameter(
		location: ParameterLocation, name: String,
		description: String = "", schema: JsonSchema? = null
	) {
		parameters.add(OpenApiParameter(
			location, name, description,
			schema
		))
	}

	fun intoJson(): OpenApiOperation {
		return OpenApiOperation(
			tags = tags.map { Tag(it) },
			summary = summary,
			description = description,
			operationId = operationId,
			deprecated = deprecated,
			parameters = parameters,
			responses = responses.mapValues {
				it.value.intoJson()
			}
		)

	}
}

class DocumentationContext(val path: DocumentationPath) {
	val ops: MutableMap<HttpMethod, DocumentationOperationContext> = mutableMapOf()
	var summary: String = ""
	var description = ""
	fun intoJson(): OpenApiRoute {
		return OpenApiRoute(
			summary,
			description,
			get = ops[HttpMethod.Get]?.intoJson(),
			put = ops[HttpMethod.Put]?.intoJson(),
			post = ops[HttpMethod.Post]?.intoJson(),
			patch = ops[HttpMethod.Patch]?.intoJson(),
			delete = ops[HttpMethod.Delete]?.intoJson(),
		)
	}

	fun createOperationNode(method: HttpMethod): DocumentationOperationContext {
		return ops.getOrPut(method) { DocumentationOperationContext(this) }
	}
}


class Documentation(config: Configuration) {
	companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, Documentation> {
		override val key: AttributeKey<Documentation> = AttributeKey("LedgerDocumentation")

		override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Documentation {
			val config = Configuration().also(configure)
			if (config.servers.isEmpty()) {
				config.servers.add(Server(
					"http://${pipeline.environment.config.host}:${pipeline.environment.config.port}",
					"Server",
				))
			}
			val plugin = Documentation(config)
			return plugin
		}
	}

	val info = config.info
	var root: RoutingNode? = null
		private set
	val servers: List<Server> = config.servers

	private val documentationNodes = mutableMapOf<DocumentationPath, DocumentationContext>()
	fun createDocumentationNode(endpoint: DocumentationEndpoint) =
		documentationNodes.getOrPut(endpoint.path) { DocumentationContext(endpoint.path) }
			.createOperationNode(endpoint.method)

	private val openApiJson by lazy {
		OpenApiModel(
			info = info,
			servers = servers,
			paths = documentationNodes.map {
				OpenApiPath(it.key.path) to it.value.intoJson()
			}.toMap()
		)
	}

	fun finalizeJson(): OpenApiModel {
		return openApiJson
	}

	fun setRootNode(routingNode: RoutingNode) {
		require(documentationNodes.isEmpty()) { "Cannot set API root node after routes have been documented: ${documentationNodes.keys}" }
		this.root = routingNode
	}

	class Configuration {
		var info: Info = Info(
			title = "Example API Docs",
			description = "Missing description",
			version = "0.0.0"
		)
		val servers: MutableList<Server> = mutableListOf()
	}
}

fun Route.docs(block: DocumentationOperationContext.() -> Unit) {
	val documentation = plugin(Documentation)
	val documentationPath = DocumentationEndpoint.createDocumentationPath(documentation.root, this)
	val node = documentation.createDocumentationNode(documentationPath)
	block(node)
}

/**
 * Mark this current routing node as API route. Note that this will not apply retroactively and all api requests must be declared relative to this one.
 */
fun Route.setApiRoot() {
	plugin(Documentation).setRootNode(this as RoutingNode)
}

