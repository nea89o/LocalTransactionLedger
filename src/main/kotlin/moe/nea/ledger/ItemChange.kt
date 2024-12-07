package moe.nea.ledger

data class ItemChange(
	val itemId: ItemId,
	val count: Double,
	val direction: ChangeDirection,
) {
	enum class ChangeDirection {
		GAINED,
		TRANSFORM,
		SYNC,
		CATALYST,
		LOST;
	}

	companion object {
		fun gainCoins(number: Double): ItemChange {
			return gain(ItemId.COINS, number)
		}

		fun gain(itemId: ItemId, amount: Number): ItemChange {
			return ItemChange(itemId, amount.toDouble(), ChangeDirection.GAINED)
		}

		fun lose(itemId: ItemId, amount: Number): ItemChange {
			return ItemChange(itemId, amount.toDouble(), ChangeDirection.LOST)
		}

		fun loseCoins(number: Double): ItemChange {
			return lose(ItemId.COINS, number)
		}
	}
}