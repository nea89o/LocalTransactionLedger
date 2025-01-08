package moe.nea.ledger.utils

import com.google.gson.reflect.TypeToken

object GsonUtil {
	inline fun <reified T> typeToken(): TypeToken<T> {
		return object : TypeToken<T>() {}
	}

}