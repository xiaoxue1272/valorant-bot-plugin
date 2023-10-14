package io.tiangou.other.jdbc

import io.tiangou.config.PluginConfig
import org.sqlite.JDBC
import java.sql.Connection
import java.sql.ResultSet
import java.util.*


internal object SqliteHelper {

    private val connection: Connection = JDBC.createConnection(PluginConfig.databaseConfig.jdbcUrl, Properties())

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



