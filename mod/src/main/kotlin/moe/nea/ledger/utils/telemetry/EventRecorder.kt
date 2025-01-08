package moe.nea.ledger.utils.telemetry

interface EventRecorder {
	companion object {
		var instance: EventRecorder? = null
	}

	fun record(event: RecordedEvent)
}