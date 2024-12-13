package moe.nea.ledger.utils.telemetry

import com.google.gson.GsonBuilder
import org.apache.logging.log4j.Logger

class LoggingEventRecorder(
	val logger: Logger,
	val logJson: Boolean
) : EventRecorder {
	companion object {
		private val gson = GsonBuilder().setPrettyPrinting().create()
	}

	override fun record(event: RecordedEvent) {
		val exc = event.context.getT<ExceptionContextValue>(CommonKeys.EXCEPTION)
		var message = "Event Recorded: " + event.context.getT<StringContext>(CommonKeys.EVENT_MESSAGE)?.message
		if (logJson) {
			message += "\n" + gson.toJson(event.context.serialize())
		}
		if (exc != null)
			logger.error(message, exc.exception)
		else
			logger.warn(message)
	}
}