package io.tiangou.other.jdbc

import io.tiangou.ValorantBotPlugin
import org.sqlite.JDBC
import java.io.File
import java.sql.Connection
import java.sql.ResultSet
import java.util.*


internal object SqliteHelper {


    private val connection: Connection = JDBC.createConnection(
        "${JDBC.PREFIX}${ValorantBotPlugin.dataFolder}${File.separator}ValorantPlugin.DB3",
        Properties()
    )

    fun execute(sql: String) = connection.createStatement().execute(sql)

    fun <T> executeQuery(sql: String, block: (ResultSet) -> T): T =
        block(connection.createStatement().executeQuery(sql))

}



