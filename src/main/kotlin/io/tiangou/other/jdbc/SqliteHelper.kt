package io.tiangou.other.jdbc

import io.tiangou.ValorantBotPlugin
import org.sqlite.JDBC
import java.io.File
import java.sql.Connection
import java.sql.ResultSet
import java.util.*


internal object SqliteHelper {

    private const val databaseName = "ValorantPlugin.DB3"

    private val connection: Connection = JDBC.createConnection(
        "${JDBC.PREFIX}${ValorantBotPlugin.dataFolder}${File.separator}$databaseName",
        Properties()
    )

    fun execute(sql: String): Boolean {
        val statement = connection.createStatement()
        val result = statement.execute(sql)
        statement.close()
        return result
    }

    fun executeGenerateKeys(sql: String): Long? {
        val statement = connection.createStatement()
        statement.execute(sql)
        val result = statement.generatedKeys?.getLong(1)
        statement.close()
        return result
    }

    fun <T> executeQuery(sql: String, block: (ResultSet) -> T?): T? {
        val statement = connection.createStatement()
        val result = block(statement.executeQuery(sql))
        statement.close()
        return result
    }


}



