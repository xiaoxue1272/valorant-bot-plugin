package io.tiangou.repository

import io.tiangou.api.ThirdPartyValorantApi
import io.tiangou.other.jdbc.SqliteHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.utils.MiraiLogger
import java.sql.ResultSet
import java.util.StringJoiner
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(val jdbcType: String, val isNotNull: Boolean = false, val extraProperties: String = "")

abstract class PersistData {

    class FieldColumnRelation(val type: KClass<*>, val column: Column)

    val columns: Map<String, FieldColumnRelation> = mutableMapOf<String, FieldColumnRelation>().apply {
        this::class.memberProperties.forEach {
            it.isAccessible = true
            it.findAnnotation<Column>()?.apply { put(it.name, FieldColumnRelation(it.returnType.classifier.cast(), this)) }
        }
    }.toMap()

    val columnsString: String = columns.toList().map { it.first }.joinToString(",")

    val table: String = this::class.simpleName!!

    val createTableIfNotExistsSql: String = run {
        "create table if not exists $table(${columns.map {
            val column = it.value.column
            "${it.key} ${column.jdbcType} ${column.extraProperties} ${if (column.isNotNull) "not null" else ""}"
        }.joinToString(",")});"
    }

    fun createTableIfNotExists(): Boolean =
        SqliteHelper.execute(createTableIfNotExistsSql)

    companion object {

        inline fun <reified T: PersistData> convertResultSet(resultSet: ResultSet): T {
            val kClass = T::class
            val instance = kClass.createInstance()
            kClass.memberProperties.forEach {
                it.isAccessible = true
                it.javaField?.set(instance, resultSet.getObject(it.name, columns[it.name]!!.type.java))
            }
            return instance
        }
    }

    fun save(): Boolean {
        val columnsJoiner = StringJoiner(",")
        val columnsValuesJoiner = StringJoiner(",")
        this.javaClass.kotlin.memberProperties.forEach {
            if (columns.containsKey(it.name)) {
                val value = it.get(this)
                columnsJoiner.add(it.name)
                columnsValuesJoiner.add("'$value'")
            }
        }
        return SqliteHelper.execute("insert or replace into $table($columnsJoiner) values($columnsValuesJoiner);")
    }

    fun queryOne(): PersistData {
        val queryConditionJoiner = StringJoiner(" and ")
        this.javaClass.kotlin.memberProperties.forEach {
            if (columns.containsKey(it.name)) {
                val value = it.get(this)
                queryConditionJoiner.add("${it.name} = '$value'")
            }
        }
        return SqliteHelper.executeQueryOne("select $columnsString from $table where $queryConditionJoiner") {
            convertResultSet<PersistData>(it)
        }
    }

}

data class WeaponSkin(
    @Column("varchar(100)", isNotNull = true, extraProperties = "primary key") val uuid: String,
    @Column("varchar(100)") val contentTiersUuid: String?,
    @Column("varchar(100)") val themeUuid: String?
) : PersistData()

data class WeaponSkinLevel(
    val uuid: String,
    val displayIcon: String?,
    val displayName: String?,
    val weaponSkinUuid: String,
) : PersistData()

data class ContentTier(
    val uuid: String,
    val displayIcon: String?,
    val highlightColor: String?,
) : PersistData()

data class Theme(
    val uuid: String,
    val displayIcon: String?
) : PersistData()

object ValorantThirdPartyDataInitializeSaver {

    private val logger: MiraiLogger = MiraiLogger.Factory.create(ValorantThirdPartyDataInitializeSaver::class)

    suspend fun init() {
        withContext(Dispatchers.IO) {
            logger.info("starting initial Valorant DataBase")
            WeaponSkin.createTableIfNotExists()
            WeaponSkinLevel.createTableIfNotExists()
            ContentTier.createTableIfNotExists()
            Theme.createTableIfNotExists()
            ThirdPartyValorantApi.AllWeaponSkin.execute().data.forEach { skin ->
                WeaponSkin.save(
                    WeaponSkin(
                        skin.uuid,
                        skin.contentTierUuid,
                        skin.themeUuid
                    )
                )
                skin.levels.forEach { level ->
                    WeaponSkinLevel.save(
                        WeaponSkinLevel(
                            level.uuid,
                            level.displayIcon,
                            level.displayName,
                            skin.uuid
                        )
                    )
                }
            }
            logger.info("skin and level data initialled")
            ThirdPartyValorantApi.AllContentTier.execute().data.forEach { contentTier ->
                ContentTier.save(
                    ContentTier(
                        contentTier.uuid,
                        contentTier.displayIcon,
                        contentTier.highlightColor
                    )
                )
            }
            logger.info("contentTiers and level data initialled")
            ThirdPartyValorantApi.AllTheme.execute().data.forEach { theme ->
                Theme.save(Theme(theme.uuid, theme.displayIcon))
            }
            logger.info("themes and level data initialled")
            logger.info("Valorant DataBase initialled successful")
        }
    }


}