package moe.nea.ledger.utils.network

import com.google.gson.reflect.TypeToken
import moe.nea.ledger.Ledger

data class Response(
	val source: Request,
	// TODO: allow other body processors, to avoid loading everything as strings
	val response: String,
	val headers: Map<String, List<String>>,
) {
	fun <T : Any> json(typ: TypeToken<T>): T {
		return Ledger.gson.fromJson(response, typ.type)
	}

	fun <T : Any> json(clazz: Class<T>): T {
		return Ledger.gson.fromJson(response, clazz)
	}
}