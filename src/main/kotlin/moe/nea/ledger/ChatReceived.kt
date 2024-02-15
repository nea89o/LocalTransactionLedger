package moe.nea.ledger

import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.Event
import java.time.Instant

data class ChatReceived(
    val message: String,
    val timestamp: Instant = Instant.now()
) : Event() {
    constructor(event: ClientChatReceivedEvent) : this(
        event.message.unformattedText
            .replace("§.".toRegex(), "")
    )
}