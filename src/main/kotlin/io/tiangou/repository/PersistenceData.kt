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
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(val jdbcType: String, val isNotNull: Boolean = false, val extraProperties: String = "")


abstract class PersistenceData<T: Any> {

    abstract val relation: ObjectTableRelation<T>

    fun save(): Boolean {
        val columnsJoiner = StringJoiner(",")
        val columnsValuesJoiner = StringJoiner(",")
        relation.columns.forEach { entry ->
            val field = this.javaClass.kotlin.memberProperties.find { it.name == entry.key }!!
            field.isAccessible = true
            val value = field.get(this)
            if (value != null && value.toString().isNotEmpty()) {
                columnsJoiner.add(entry.key)
                columnsValuesJoiner.add("'$value'")
            }
        }
        return SqliteHelper.execute("insert or replace into ${relation.table}($columnsJoiner) values($columnsValuesJoiner);")
    }

    private fun buildQueryCondition(): String {
        val queryConditionJoiner = StringJoiner(" and ")
        relation.columns.forEach { entry ->
            val field = this.javaClass.kotlin.memberProperties.find { it.name == entry.key }!!
            field.isAccessible = true
            val value = field.get(this)
            if (value != null && value.toString().isNotEmpty()) {
                queryConditionJoiner.add("${entry.key} = '$value'")
            }
        }
        return queryConditionJoiner.toString()
    }

    fun queryOne(): T = SqliteHelper.executeQueryOne(
        "select ${relation.columnsString} from ${relation.table} where ${buildQueryCondition()}",
        ::convertResultSet
    )


    private fun convertResultSet(resultSet: ResultSet): T {
        val instance = relation.type.createInstance()
        relation.columns.forEach { entry ->
            relation.type.memberProperties.find { it.name == entry.key }!!.apply {
                isAccessible = true
            }.javaField!!.set(instance, resultSet.getObject(entry.key, entry.value.type.java))
        }
        return instance
    }

    companion object {
        fun createTable(relation: ObjectTableRelation<*>): Boolean = SqliteHelper.execute(relation.createTableSql)

    }

}

abstract class ObjectTableRelation<T: Any>(internal val type: KClass<T>) {

    class FieldColumnRelation(val type: KClass<*>, val column: Column)

    val columns: Map<String, FieldColumnRelation> = mutableMapOf<String, FieldColumnRelation>().apply {
        type.memberProperties.forEach {
            it.javaField!!.getAnnotation(Column::class.java)?.apply {
                put(it.name, FieldColumnRelation(it.returnType.classifier.cast(), this))
            }
        }
    }.toMap()

    internal val columnsString: String = columns.toList().map { it.first }.joinToString(",")

    internal val table: String = type.simpleName!!

    internal val createTableSql: String = run {
        "create table if not exists $table(${columns.map {
            val column = it.value.column
            "${it.key} ${column.jdbcType} ${column.extraProperties} ${if (column.isNotNull) "not null" else ""}"
        }.joinToString(",")});"
    }
}

data class WeaponSkin(
    @Column("varchar(100)", true, "primary key")
    val uuid: String? = null,
    @Column("varchar(100)")
    val contentTiersUuid: String? = null,
    @Column("varchar(100)")
    val themeUuid: String? = null
): PersistenceData<WeaponSkin>() {

    override val relation: ObjectTableRelation<WeaponSkin> = WeaponSkin

    companion object: ObjectTableRelation<WeaponSkin>(WeaponSkin::class)

}

data class WeaponSkinLevel(
    @Column("varchar(100)", true, "primary key")val uuid: String? = null,
    @Column("varchar(255)") val displayIcon: String? = null,
    @Column("varchar(100)") val displayName: String? = null,
    @Column("varchar(100)", true) val weaponSkinUuid: String? = null,
): PersistenceData<WeaponSkinLevel>() {

    override val relation: ObjectTableRelation<WeaponSkinLevel> = WeaponSkinLevel

    companion object: ObjectTableRelation<WeaponSkinLevel>(WeaponSkinLevel::class)

}

data class ContentTier(
    @Column("varchar(100)", true) val uuid: String? = null,
    @Column("varchar(255)") val displayIcon: String? = null,
    @Column("varchar(255)") val highlightColor: String? = null,
) : PersistenceData<ContentTier>() {

    override val relation: ObjectTableRelation<ContentTier> = ContentTier

    companion object: ObjectTableRelation<ContentTier>(ContentTier::class)

}

data class Theme(
    @Column("varchar(100)", true) val uuid: String? = null,
    @Column("varchar(255)") val displayIcon: String? = null
) : PersistenceData<Theme>() {

    override val relation: ObjectTableRelation<Theme> = Theme

    companion object: ObjectTableRelation<Theme>(Theme::class)

}

object ValorantThirdPartyDataInitializeSaver {

    private val logger: MiraiLogger = MiraiLogger.Factory.create(ValorantThirdPartyDataInitializeSaver::class)

    suspend fun init() {
        withContext(Dispatchers.IO) {
            logger.info("starting initial Valorant DataBase")
            PersistenceData.createTable(WeaponSkin)
            PersistenceData.createTable(WeaponSkinLevel)
            PersistenceData.createTable(ContentTier)
            PersistenceData.createTable(Theme)
            ThirdPartyValorantApi.AllWeaponSkin.execute().data.forEach { skin ->
                WeaponSkin(
                    skin.uuid,
                    skin.contentTierUuid,
                    skin.themeUuid
                ).save()
                skin.levels.forEach { level ->
                    WeaponSkinLevel(
                        level.uuid,
                        level.displayIcon,
                        level.displayName,
                        skin.uuid
                    ).save()
                }
            }
            logger.info("skin and level data initialled")
            ThirdPartyValorantApi.AllContentTier.execute().data.forEach { contentTier ->
                ContentTier(
                    contentTier.uuid,
                    contentTier.displayIcon,
                    contentTier.highlightColor
                ).save()
            }
            logger.info("contentTiers and level data initialled")
            ThirdPartyValorantApi.AllTheme.execute().data.forEach { theme ->
                Theme(theme.uuid, theme.displayIcon).save()
            }
            logger.info("themes and level data initialled")
            logger.info("Valorant DataBase initialled successful")
        }
    }


}