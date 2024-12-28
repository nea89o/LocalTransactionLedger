package moe.nea.ledger

@JvmInline
value class ItemId(
	val string: String
) {
	fun singleItem(): Pair<ItemId, Double> {
		return withStackSize(1)
	}

	fun withStackSize(size: Number): Pair<ItemId, Double> {
		return Pair(this, size.toDouble())
	}


	companion object {
		fun skill(skill: String) = ItemId("SKYBLOCK_SKILL_$skill")

		val GARDEN = skill("GARDEN")
		val FARMING = skill("FARMING")
		val GEMSTONE_POWDER = ItemId("SKYBLOCK_POWDER_GEMSTONE")
		val MITHRIL_POWDER = ItemId("SKYBLOCK_POWDER_MITHRIL")
		val GOLD_ESSENCE = ItemId("ESSENCE_GOLD")
		val DRAGON_ESSENCE = ItemId("ESSENCE_DRAGON")
		val PELT = ItemId("SKYBLOCK_PELT")
		val COINS = ItemId("SKYBLOCK_COIN")
		val FINE_FLOUR = ItemId("FINE_FLOUR")
		val BITS = ItemId("SKYBLOCK_BIT")
		val COPPER = ItemId("SKYBLOCK_COPPER")
		val NIL = ItemId("SKYBLOCK_NIL")
		val ARCHFIEND_LOW_CLASS = ItemId("ARCHFIEND_DICE")
		val ARCHFIEND_HIGH_CLASS = ItemId("HIGH_CLASS_ARCHFIEND_DICE")
		val CAP_EYEDROPS = ItemId("CAPSAICIN_EYEDROPS_NO_CHARGES")
		val ARCHFIEND_DYE = ItemId("DYE_ARCHFIEND")
		val SLEEPING_EYE = ItemId("SLEEPING_EYE")
		val SUMMONING_EYE = ItemId("SUMMONING_EYE")
		val DUNGEON_CHEST_KEY = ItemId("DUNGEON_CHEST_KEY")
		val BOOSTER_COOKIE = ItemId("BOOSTER_COOKIE")
		val KISMET_FEATHER = ItemId("KISMET_FEATHER")
	}
}