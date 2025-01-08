package moe.nea.ledger.utils

import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

abstract class BorderedTextTracker {

	val genericBorderExit = "▬{10,}".toPattern()

	@Inject
	lateinit var errorUtil: ErrorUtil
	var stack: MutableList<ChatReceived>? = null


	@SubscribeEvent
	fun receiveText(event: ChatReceived) {
		if (stack != null && shouldExit(event)) {
			exit()
			return
		}
		if (shouldEnter(event)) {
			if (stack != null) {
				errorUtil.reportAdHoc("Double entered a bordered message")
				exit()
			}
			stack = mutableListOf()
		}
		stack?.add(event)
	}

	private fun exit() {
		onBorderedTextFinished(stack!!)
		stack = null
	}

	abstract fun shouldEnter(event: ChatReceived): Boolean
	abstract fun shouldExit(event: ChatReceived): Boolean
	abstract fun onBorderedTextFinished(enclosed: List<ChatReceived>)

}