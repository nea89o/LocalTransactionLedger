package moe.nea.ledger

import moe.nea.ledger.database.DBItemEntry
import moe.nea.ledger.database.ResultRow
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.IChatComponent

data class ItemChange(
	val itemId: ItemId,
	val count: Double,
	val direction: ChangeDirection,
) {
	fun formatChat(): IChatComponent {
		return ChatComponentText(" ")
			.appendSibling(direction.chatFormat)
			.appendText(" ")
			.appendSibling(ChatComponentText("$count").setChatStyle(ChatStyle().setColor(EnumChatFormatting.WHITE)))
			.appendSibling(ChatComponentText("x").setChatStyle(ChatStyle().setColor(EnumChatFormatting.DARK_GRAY)))
			.appendText(" ")
			.appendSibling(ChatComponentText(itemId.string).setChatStyle(ChatStyle().setParentStyle(ChatStyle().setColor(
				EnumChatFormatting.WHITE))))
	}

	enum class ChangeDirection {
		GAINED,
		TRANSFORM,
		SYNC,
		CATALYST,
		LOST;

		val chatFormat by lazy { formatChat0() }
		private fun formatChat0(): IChatComponent {
			val (text, color) = when (this) {
				GAINED -> "+" to EnumChatFormatting.GREEN
				TRANSFORM -> "~" to EnumChatFormatting.YELLOW
				SYNC -> "=" to EnumChatFormatting.BLUE
				CATALYST -> "*" to EnumChatFormatting.DARK_PURPLE
				LOST -> "-" to EnumChatFormatting.RED
			}
			return ChatComponentText(text)
				.setChatStyle(
					ChatStyle()
						.setColor(color)
						.setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT,
						                              ChatComponentText(name).setChatStyle(ChatStyle().setColor(color)))))
		}
	}

	companion object {
		fun gainCoins(number: Double): ItemChange {
			return gain(ItemId.COINS, number)
		}

		fun unpair(direction: ChangeDirection, pair: Pair<ItemId, Double>): ItemChange {
			return ItemChange(pair.first, pair.second, direction)
		}

		fun unpairGain(pair: Pair<ItemId, Double>) = unpair(ChangeDirection.GAINED, pair)

		fun gain(itemId: ItemId, amount: Number): ItemChange {
			return ItemChange(itemId, amount.toDouble(), ChangeDirection.GAINED)
		}

		fun lose(itemId: ItemId, amount: Number): ItemChange {
			return ItemChange(itemId, amount.toDouble(), ChangeDirection.LOST)
		}

		fun loseCoins(number: Double): ItemChange {
			return lose(ItemId.COINS, number)
		}

		fun from(result: ResultRow): ItemChange {
			return ItemChange(
				result[DBItemEntry.itemId],
				result[DBItemEntry.size],
				result[DBItemEntry.mode],
			)
		}
	}
}