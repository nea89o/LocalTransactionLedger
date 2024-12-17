package moe.nea.ledger.utils.network

import moe.nea.ledger.utils.ErrorUtil
import moe.nea.ledger.utils.di.Inject
import java.net.URL
import java.net.URLConnection
import java.security.KeyStore
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class RequestUtil @Inject constructor(val errorUtil: ErrorUtil) {

	private fun createSSLContext(): SSLContext? = errorUtil.catch {
		val keyStorePath = RequestUtil::class.java.getResourceAsStream("/ledgerkeystore.jks")
			?: error("Could not locate keystore")
		val keyStore = KeyStore.getInstance("JKS")
		keyStore.load(keyStorePath, "neuneu".toCharArray())
		val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
		val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
		kmf.init(keyStore, null)
		tmf.init(keyStore)
		val ctx = SSLContext.getInstance("TLS")
		ctx.init(kmf.keyManagers, tmf.trustManagers, null)
		return@catch ctx
	}

	val sslContext = createSSLContext()

	fun enhanceConnection(connection: URLConnection) {
		if (connection is HttpsURLConnection && sslContext != null) {
			connection.sslSocketFactory = sslContext.socketFactory
		}
	}

	fun createRequest(url: String) = createRequest(URL(url))
	fun createRequest(url: URL) = Request(url, Request.Method.GET, null, mapOf())

	fun executeRequest(request: Request): Response {
		val connection = request.url.openConnection()
		enhanceConnection(connection)
		connection.setRequestProperty("accept-encoding", "gzip")
		request.headers.forEach { (k, v) ->
			connection.setRequestProperty(k, v)
		}
		if (request.body != null) {
			connection.getOutputStream().write(request.body.encodeToByteArray())
			connection.getOutputStream().close()
		}
		var stream = connection.getInputStream()
		if (connection.contentEncoding == "gzip") {
			stream = GZIPInputStream(stream)
		}
		val text = stream.bufferedReader().readText()
		stream.close()
		// Do NOT call connection.disconnect() to allow for connection reuse
		return Response(request, text, connection.headerFields)
	}


}