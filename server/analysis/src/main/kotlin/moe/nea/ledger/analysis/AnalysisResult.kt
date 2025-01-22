package moe.nea.ledger.analysis

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisResult(
	val visualizations: List<Visualization>
)