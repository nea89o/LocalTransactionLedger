package moe.nea.ledger.analysis

import moe.nea.ledger.database.DBLogEntry
import moe.nea.ledger.database.Query
import moe.nea.ledger.database.columns.DBUlid
import moe.nea.ledger.database.sql.Clause
import moe.nea.ledger.utils.ULIDWrapper
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

interface AnalysisFilter {
	fun applyTo(query: Query) {
		query.where(Clause { column(DBLogEntry.transactionId) ge value(DBUlid, ULIDWrapper.lowerBound(startWindow)) })
			.where(Clause { column(DBLogEntry.transactionId) le value(DBUlid, ULIDWrapper.upperBound(endWindow)) })
//TODO: 			.where(Clause { column(DBLogEntry.profileId) inList profiles })
	}

	fun timeZone(): ZoneId {
		return ZoneId.systemDefault()
	}

	val startWindow: Instant
	val endWindow: Instant
	val profiles: List<UUID>
}
