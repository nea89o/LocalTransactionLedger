package moe.nea.ledger.database

import java.sql.Connection
import java.sql.PreparedStatement

fun Connection.prepareAndLog(statement: String): PreparedStatement {
	println("Preparing to execute $statement")
	return prepareStatement(statement)
}




