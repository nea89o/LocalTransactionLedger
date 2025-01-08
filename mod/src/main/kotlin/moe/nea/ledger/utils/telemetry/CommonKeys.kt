package moe.nea.ledger.utils.telemetry

object CommonKeys {
	val EVENT_MESSAGE = "event_message"
	val EXCEPTION = "event_exception"
	val COMMIT_VERSION = "version_commit"
	val VERSION = "version"
	val PHASE = "phase" // TODO: add a sort of "manual" stacktrace with designated function phases
}