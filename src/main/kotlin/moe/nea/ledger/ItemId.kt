package moe.nea.ledger

data class ItemId(
	val string: String
) {
	fun singleItem(): Pair<ItemId, Double> {
		return withStackSize(1)
	}

	fun withStackSize(size: Number): Pair<ItemId, Double> {
		return Pair(this, size.toDouble())
	}


	companion object {

		@JvmStatic
		fun forName(string: String) = ItemId(string)
		fun skill(skill: String) = ItemId("SKYBLOCK_SKILL_$skill")

		val GARDEN = skill("GARDEN")
		val FARMING = skill("FARMING")


		val COINS = ItemId("SKYBLOCK_COIN")
		val GEMSTONE_POWDER = ItemId("SKYBLOCK_POWDER_GEMSTONE")
		val MITHRIL_POWDER = ItemId("SKYBLOCK_POWDER_MITHRIL")
		val NIL = ItemId("SKYBLOCK_NIL")
	}
}