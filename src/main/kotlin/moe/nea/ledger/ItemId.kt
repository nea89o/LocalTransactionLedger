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


		val ARCHFIEND_DYE = ItemId("DYE_ARCHFIEND")
		val ARCHFIEND_HIGH_CLASS = ItemId("HIGH_CLASS_ARCHFIEND_DICE")
		val ARCHFIEND_LOW_CLASS = ItemId("ARCHFIEND_DICE")
		val BITS = ItemId("SKYBLOCK_BIT")
		val BOOSTER_COOKIE = ItemId("BOOSTER_COOKIE")
		val CAP_EYEDROPS = ItemId("CAPSAICIN_EYEDROPS_NO_CHARGES")
		val COINS = ItemId("SKYBLOCK_COIN")
		val COPPER = ItemId("SKYBLOCK_COPPER")
		val DRAGON_ESSENCE = ItemId("ESSENCE_DRAGON")
		val DUNGEON_CHEST_KEY = ItemId("DUNGEON_CHEST_KEY")
		val FINE_FLOUR = ItemId("FINE_FLOUR")
		val GEMSTONE_POWDER = ItemId("SKYBLOCK_POWDER_GEMSTONE")
		val GOD_POTION = ItemId("GOD_POTION_2")
		val GOLD_ESSENCE = ItemId("ESSENCE_GOLD")
		val KISMET_FEATHER = ItemId("KISMET_FEATHER")
		val MITHRIL_POWDER = ItemId("SKYBLOCK_POWDER_MITHRIL")
		val NIL = ItemId("SKYBLOCK_NIL")
		val PELT = ItemId("SKYBLOCK_PELT")
		val SLEEPING_EYE = ItemId("SLEEPING_EYE")
		val SUMMONING_EYE = ItemId("SUMMONING_EYE")
	}
}