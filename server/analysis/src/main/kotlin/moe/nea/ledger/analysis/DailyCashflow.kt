package moe.nea.ledger.analysis

import com.google.auto.service.AutoService
import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.TransactionType
import moe.nea.ledger.database.DBItemEntry
import moe.nea.ledger.database.DBLogEntry
import moe.nea.ledger.database.sql.Clause
import java.sql.Connection
import java.time.LocalDate
import kotlin.collections.component1
import kotlin.collections.component2

@AutoService(Analysis::class)
class DailyCashflow : Analysis {
    override val id: String
        get() = "daily-cashflow"
    override val name: String
        get() = "Daily Cashflow"

    override fun perform(
        database: Connection,
        filter: AnalysisFilter
    ): AnalysisResult {
        val query = DBLogEntry.from(database)
            .join(DBItemEntry, Clause { column(DBItemEntry.transactionId) eq column(DBLogEntry.transactionId) })
            .where(Clause { column(DBItemEntry.itemId) eq ItemId.COINS })
            .select(DBItemEntry.size, DBLogEntry.transactionId)
        filter.applyTo(query)
        val spentThatDay = mutableMapOf<LocalDate, Double>()
        for (resultRow in query) {
            val timestamp = resultRow[DBLogEntry.transactionId].getTimestamp()
            val damage = resultRow[DBItemEntry.size]
            val localZone = filter.timeZone()
            val localDate = timestamp.atZone(localZone).toLocalDate()
            spentThatDay.merge(localDate, damage) { a, b -> a + b }
        }
        return AnalysisResult(
            listOf(
                Visualization(
                    "Daily Cashflow",
                    xLabel = "Day",
                    yLabel = "Coins +/-",
                    dataPoints = spentThatDay.entries.map { (k, v) ->
                        DataPoint(k.atTime(12, 0).atZone(filter.timeZone()).toInstant(), v)
                    }
                )
            )
        )
    }
}