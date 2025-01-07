package moe.nea.ledger

import moe.nea.ledger.database.sql.ANDExpression
import moe.nea.ledger.database.sql.BooleanExpression
import moe.nea.ledger.database.sql.Clause
import moe.nea.ledger.database.DBItemEntry
import moe.nea.ledger.database.DBLogEntry
import moe.nea.ledger.database.Database
import moe.nea.ledger.utils.ULIDWrapper
import moe.nea.ledger.utils.di.Inject
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.EnumChatFormatting

class QueryCommand : CommandBase() {
	override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
		return true
	}

	override fun getCommandName(): String {
		return "ledger"
	}

	override fun getCommandUsage(sender: ICommandSender?): String {
		return ""
	}

	override fun getCommandAliases(): List<String> {
		return listOf("lgq")
	}

	@Inject
	lateinit var logger: LedgerLogger

	override fun processCommand(sender: ICommandSender, args: Array<out String>) {
		if (args.isEmpty()) {
			logger.printOut("§eHere is how you can look up transactions:")
			logger.printOut("")
			logger.printOut("§f- §e/ledger withitem %POTATO%")
			logger.printOut("    §aLook up transactions involving potatoes!")
			logger.printOut("§f- §e/ledger withitem ENCHANTED_POTATO")
			logger.printOut("    §aLook up transactions involving just enchanted potatoes!")
			logger.printOut("§f- §e/ledger withitem %POTATO% withitem %CARROT%")
			logger.printOut("    §aLook up transactions involving potatoes or carrots!")
			logger.printOut("§f- §e/ledger withtype AUCTION_SOLD")
			logger.printOut("    §aLook up transactions of sold auctions!")
			logger.printOut("§f- §e/ledger withtype AUCTION_SOLD withitem CRIMSON%")
			logger.printOut("    §aLook up sold auctions involving crimson armor pieces!")
			logger.printOut("")
			logger.printOut("§eFilters of the same type apply using §aOR§e and loggers of different types apply using §aAND§e.")
			logger.printOut("§eYou can use % as a wildcard!")
			return
		}
		val p = parseArgs(args)
		when (p) {
			is ParseResult.Success -> {
				executeQuery(p)
			}

			is ParseResult.UnknownFilter -> {
				logger.printOut("§cUnknown filter name ${p.start}. Available filter names are: ${mFilters.keys.joinToString()}")
			}

			is ParseResult.MissingArg -> {
				logger.printOut("§cFilter ${p.filterM.name} is missing an argument.")
			}
		}
	}

	override fun addTabCompletionOptions(
		sender: ICommandSender,
		args: Array<out String>,
		pos: BlockPos
	): MutableList<String>? {
		when (val p = parseArgs(args)) {
			is ParseResult.MissingArg -> return null
			is ParseResult.Success -> return p.lastFilterM.tabComplete(args.last())
			is ParseResult.UnknownFilter -> return getListOfStringsMatchingLastWord(args, mFilters.keys)
		}
	}

	@Inject
	lateinit var database: Database
	private fun executeQuery(parse: ParseResult.Success) {
		val grouped = parse.filters
		val query = DBLogEntry.from(database.connection)
			.select(DBLogEntry.type, DBLogEntry.transactionId)
			.join(DBItemEntry, on = Clause { column(DBLogEntry.transactionId) eq column(DBItemEntry.transactionId) })
		for (value in grouped.values) {
			query.where(ANDExpression(value))
		}
		query.limit(80u)
		val dedup = mutableSetOf<ULIDWrapper>()
		query.forEach {
			val type = it[DBLogEntry.type]
			val transactionId = it[DBLogEntry.transactionId]
			if (!dedup.add(transactionId)) {
				return@forEach
			}
			val timestamp = transactionId.getTimestamp()
			val items = DBItemEntry.selectAll(database.connection)
				.where(Clause { column(DBItemEntry.transactionId) eq string(transactionId.wrapped) })
				.map { ItemChange.from(it) }
			val text = ChatComponentText("")
				.setChatStyle(ChatStyle().setColor(EnumChatFormatting.YELLOW))
				.appendSibling(
					ChatComponentText(type.name)
						.setChatStyle(ChatStyle().setColor(EnumChatFormatting.GREEN))
				)
				.appendText(" on ")
				.appendSibling(timestamp.formatChat())
				.appendText("\n")
				.appendSibling(
					ChatComponentText(transactionId.wrapped).setChatStyle(ChatStyle().setColor(EnumChatFormatting.DARK_GRAY))
				)
			for (item in items) {
				text.appendText("\n")
					.appendSibling(item.formatChat())
			}
			text.appendText("\n")
			logger.printOut(text)
		}
	}

	sealed interface ParseResult {
		data class UnknownFilter(val start: String) : ParseResult
		data class MissingArg(val filterM: FilterM) : ParseResult
		data class Success(val lastFilterM: FilterM, val filters: Map<FilterM, List<BooleanExpression>>) : ParseResult
	}

	fun parseArgs(args: Array<out String>): ParseResult {
		require(args.isNotEmpty())
		val arr = args.iterator()
		val filters = mutableMapOf<FilterM, MutableList<BooleanExpression>>()
		var lastFilterM: FilterM? = null
		while (arr.hasNext()) {
			val filterName = arr.next()
			val filterM = mFilters[filterName]
			if (filterM == null) {
				return ParseResult.UnknownFilter(filterName)
			}
			if (!arr.hasNext()) {
				return ParseResult.MissingArg(filterM)
			}
			filters.getOrPut(filterM, ::mutableListOf).add(filterM.getFilter(arr.next()))
			lastFilterM = filterM
		}
		return ParseResult.Success(lastFilterM!!, filters)
	}


	val mFilters = listOf(TypeFilter, ItemFilter).associateBy { it.name }

	object TypeFilter : FilterM {
		override val name: String
			get() = "withtype"

		override fun getFilter(text: String): BooleanExpression {
			val preparedText = "%" + text.trim('%') + "%"
			return Clause { column(DBLogEntry.type) like preparedText }
		}

		override fun tabComplete(partialArg: String): MutableList<String> {
			return TransactionType.entries.asSequence().map { it.name }.filter { partialArg in it }.toMutableList()
		}
	}

	object ItemFilter : FilterM {
		override val name: String
			get() = "withitem"

		private val itemIdProvider = Ledger.di.provide<ItemIdProvider>() // TODO: close this escape hatch
		override fun getFilter(text: String): BooleanExpression {
			return Clause { column(DBItemEntry.itemId) like text }
		}

		override fun tabComplete(partialArg: String): MutableList<String>? {
			return itemIdProvider.getKnownItemIds()
				.asSequence()
				.map { it.string }
				.filter { partialArg in it }
				.take(100)
				.toMutableList()
		}
	}

	interface FilterM {
		val name: String
		fun getFilter(text: String): BooleanExpression
		fun tabComplete(partialArg: String): MutableList<String>?
//		fun tabCompleteFilter() TODO
	}

}