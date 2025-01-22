package moe.nea.ledger.analysis

import java.sql.Connection

interface Analysis {
	val id: String
	val name: String
	fun perform(database: Connection, filter: AnalysisFilter): AnalysisResult
}