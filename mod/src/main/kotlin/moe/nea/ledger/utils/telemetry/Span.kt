package moe.nea.ledger.utils.telemetry

class Span(val parent: Span?) : AutoCloseable {
	companion object {
		val rootSpan = Span(null)
		private val _current = object : InheritableThreadLocal<Span>() {
			override fun initialValue(): Span {
				return Span(rootSpan)
			}

			override fun childValue(parentValue: Span?): Span {
				return parentValue?.forkNewRoot() ?: initialValue()
			}
		}

		fun current(): Span {
			return _current.get()
		}
	}

	private val data = Context()

	// TODO : replace string key with a SpanKey<T> class
	fun add(key: String, value: ContextValue) {
		data.add(key, value)
	}

	/**
	 * Create a sub span, and [enter] it, with the given values.
	 */
	fun <T> enterWith(vararg pairs: Pair<String, ContextValue>, block: Span.() -> T): T {
		return enter().use { span ->
			pairs.forEach { (k, value) ->
				span.add(k, value)
			}
			block(span)
		}
	}

	/**
	 * Create a sub span, to attach some additional context, without modifying the [current] at all.
	 */
	fun forkWith(vararg pairs: Pair<String, ContextValue?>): Span {
		val newSpan = fork()
		for ((key, value) in pairs) {
			if (value == null) continue
			newSpan.add(key, value)
		}
		return newSpan
	}

	/**
	 * Create a sub span, to which additional context can be added. This context will receive updates from its parent,
	 * and will be set as the [current]. To return to the parent, either call [exit] on the child. Or use inside of a
	 * [use] block.
	 */
	fun enter(): Span {
		require(_current.get() == this)
		return fork().enterSelf()
	}

	/**
	 * Force [enter] this span, without creating a subspan. This bypasses checks like parent / child being the [current].
	 */
	fun enterSelf(): Span {
		_current.set(this)
		return this
	}

	/**
	 * Creates a temporary sub span, to which additional context can be added. This context will receive updates from
	 * its parent, but will not be set as the [current].
	 */
	fun fork(): Span {
		return Span(this)
	}

	/**
	 * Create a new root span, that will not receive any updates from the current span, but will have all the same
	 * context keys associated.
	 */
	fun forkNewRoot(): Span {
		val newRoot = Span(null)
		newRoot.data.data.putAll(collectContext().data)
		return newRoot
	}

	/**
	 * Collect the context, including all parent context
	 */
	fun collectContext(): Context {
		if (parent != null)
			return data.combineWith(parent.collectContext())
		return data
	}

	/**
	 * Exit an [entered][enter] span, returning back to the parent context, and discard any current keys.
	 */
	fun exit() {
		require(parent != null)
		require(_current.get() == this)
		_current.set(parent)
	}

	/**
	 * [AutoCloseable] implementation for [exit]
	 */
	override fun close() {
		return exit()
	}

	/**
	 * Record an empty event given the context. This indicates nothing except for "I was here".
	 * @see recordMessageEvent
	 * @see recordException
	 */
	fun recordEmptyTrace(recorder: EventRecorder) {
		recorder.record(RecordedEvent(collectContext()))
	}

	/**
	 * Record a message with the key `"event_message"` to the recorder
	 */
	fun recordMessageEvent(
		recorder: EventRecorder,
		message: String
	) {
		forkWith(CommonKeys.EVENT_MESSAGE to ContextValue.string(message))
			.recordEmptyTrace(recorder)
	}

	/**
	 * Record an exception to the recorder
	 */
	fun recordException(
		recorder: EventRecorder,
		exception: Throwable,
		message: String? = null
	) {
		forkWith(
			CommonKeys.EVENT_MESSAGE to message?.let(ContextValue::string),
			CommonKeys.EXCEPTION to ExceptionContextValue(exception),
		).recordEmptyTrace(recorder)
	}

}