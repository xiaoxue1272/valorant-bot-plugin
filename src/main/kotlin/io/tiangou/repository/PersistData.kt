package io.tiangou.repository

import io.tiangou.api.ThirdPartyValorantApi
import io.tiangou.other.jdbc.SqliteManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.utils.MiraiLogger
import java.sql.ResultSet

interface TableOperator<T> {

    val columns: String

    val table: String

    fun createTableIfNotExists(): String

    fun save(data: T): String

    fun convertResultSet(resultSet: ResultSet): T
}

data class WeaponSkin(
    val uuid: String,
    val contentTiersUuid: String?,
    val themeUuid: String?
) {

    companion object : TableOperator<WeaponSkin> {

        override val columns: String = "uuid, contentTiersUuid, themeUuid"

        override val table: String = WeaponSkin::class.simpleName!!

        fun queryByUUID(uuid: String): String =
            "select $columns from $table where uuid = '$uuid';"

        override fun createTableIfNotExists(): String =
            "create table if not exists $table(" +
                    "uuid varchar(100) primary key not null," +
                    "contentTiersUuid varchar(100)," +
                    "themeUuid varchar(100)" +
                    ");"

        override fun save(data: WeaponSkin): String =
            "insert or replace into $table($columns) values('${data.uuid}', '${data.contentTiersUuid}', '${data.themeUuid}');"

        override fun convertResultSet(resultSet: ResultSet): WeaponSkin =
            WeaponSkin(
                resultSet.getString("uuid"),
                resultSet.getString("contentTiersUuid"),
                resultSet.getString("themeUuid"),
            )
    }

}

data class WeaponSkinLevel(
    val uuid: String,
    val displayIcon: String?,
    val displayName: String?,
    val weaponSkinUuid: String,
) {

    companion object : TableOperator<WeaponSkinLevel> {

        override val columns: String = "uuid, displayIcon, displayName, weaponSkinUuid"

        override val table: String = WeaponSkinLevel::class.simpleName!!

        fun queryByUUID(uuid: String): String =
            "select $columns from $table where uuid = '$uuid';"

        override fun createTableIfNotExists(): String =
            "create table if not exists $table(" +
                    "uuid varchar(100) primary key not null," +
                    "displayIcon varchar(255)," +
                    "displayName varchar(100)," +
                    "weaponSkinUuid varchar(100) not null" +
                    ");"

        override fun save(data: WeaponSkinLevel): String =
            "insert or replace into $table($columns) values('${data.uuid}', '${data.displayIcon}', '${data.displayName}', '${data.weaponSkinUuid}')"

        override fun convertResultSet(resultSet: ResultSet): WeaponSkinLevel =
            WeaponSkinLevel(
                resultSet.getString("uuid"),
                resultSet.getString("displayIcon"),
                resultSet.getString("displayName"),
                resultSet.getString("weaponSkinUuid")
            )
    }
}

data class ContentTier(
    val uuid: String,
    val displayIcon: String?,
    val highlightColor: String?,
) {

    companion object : TableOperator<ContentTier> {

        override val columns: String = "uuid, displayIcon, highlightColor"

        override val table: String = ContentTier::class.simpleName!!

        fun queryByUUID(uuid: String): String =
            "select $columns from $table where uuid = '$uuid';"

        override fun createTableIfNotExists(): String =
            "create table if not exists $table(" +
                    "uuid varchar(100) primary key not null," +
                    "displayIcon varchar(255)," +
                    "highlightColor varchar(100)" +
                    ");"

        override fun save(data: ContentTier): String =
            "insert or replace into $table($columns) values('${data.uuid}', '${data.displayIcon}', '${data.highlightColor}');"

        override fun convertResultSet(resultSet: ResultSet): ContentTier =
            ContentTier(
                resultSet.getString("uuid"),
                resultSet.getString("displayIcon"),
                resultSet.getString("highlightColor")
            )
    }

}

data class Theme(
    val uuid: String,
    val displayIcon: String?
) {

    companion object : TableOperator<Theme> {

        override val columns: String = "uuid, displayIcon"

        override val table: String = Theme::class.simpleName!!

        fun queryByUUID(uuid: String): String =
            "select $columns from $table where uuid = '$uuid';"

        override fun createTableIfNotExists(): String =
            "create table if not exists $table(" +
                    "uuid varchar(100) primary key not null," +
                    "displayIcon varchar(255)" +
                    ");"

        override fun save(data: Theme): String =
            "insert or replace into $table($columns) values('${data.uuid}', '${data.displayIcon}');"

        override fun convertResultSet(resultSet: ResultSet): Theme =
            Theme(resultSet.getString("uuid"), resultSet.getString("displayIcon"))
    }

}

object PersistDataInitialSaver {

    private val logger: MiraiLogger = MiraiLogger.Factory.create(PersistDataInitialSaver::class)

    suspend fun init() {
        withContext(Dispatchers.IO) {
            logger.info("starting initial Valorant DataBase")
            WeaponSkin.apply { SqliteManager.execute(createTableIfNotExists()) }
            WeaponSkinLevel.apply { SqliteManager.execute(createTableIfNotExists()) }
            ContentTier.apply { SqliteManager.execute(createTableIfNotExists()) }
            Theme.apply { SqliteManager.execute(createTableIfNotExists()) }
            ThirdPartyValorantApi.AllWeaponSkin.execute().data.forEach { skin ->
                SqliteManager.execute(
                    WeaponSkin.save(
                        WeaponSkin(
                            skin.uuid,
                            skin.contentTierUuid,
                            skin.themeUuid
                        )
                    )
                )
                skin.levels.forEach { level ->
                    SqliteManager.execute(
                        WeaponSkinLevel.save(
                            WeaponSkinLevel(
                                level.uuid,
                                level.displayIcon,
                                level.displayName,
                                skin.uuid
                            )
                        )
                    )
                }
            }
            logger.info("skin and level data initialled")
            ThirdPartyValorantApi.AllContentTier.execute().data.forEach { contentTier ->
                SqliteManager.execute(
                    ContentTier.save(
                        ContentTier(
                            contentTier.uuid,
                            contentTier.displayIcon,
                            contentTier.highlightColor
                        )
                    )
                )
            }
            logger.info("contentTiers and level data initialled")
            ThirdPartyValorantApi.AllTheme.execute().data.forEach { theme ->
                SqliteManager.execute(Theme.save(Theme(theme.uuid, theme.displayIcon)))
            }
            logger.info("themes and level data initialled")
            logger.info("Valorant DataBase initialled successful")
        }
    }


}