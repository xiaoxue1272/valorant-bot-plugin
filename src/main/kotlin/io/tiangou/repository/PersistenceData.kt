package io.tiangou.repository

import io.tiangou.api.ThirdPartyValorantApi
import io.tiangou.other.jdbc.SqliteHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.utils.MiraiLogger
import java.sql.ResultSet
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * 表字段注解
 * 表示该属性是表的一个字段
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(val jdbcType: String, val isNotNull: Boolean = false, val extraProperties: String = "")

/**
 * 单一索引类型注解
 * 标识该字段在表中是一个索引
 *
 * 据我所知 唯一索引应该会在字段指定为unique时自动创建(主键也是), 且组合索引有另外的注解, 所以该注解仅用于创建普通索引
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Index

/**
 * 组合索引 注解
 * 目前没用
 * 后面可能会用
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CombinedIndex(vararg val columns: String)

abstract class PersistenceData<T : Any> {

    abstract val relation: ObjectTableRelation<T>

    private fun buildSaveSql(): String {
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
        return "insert or ignore into ${relation.table}($columnsJoiner) values($columnsValuesJoiner);"
    }

    fun save(): Boolean {
        return SqliteHelper.execute(buildSaveSql())
    }

    fun saveAndReturnId(): Long {
        return SqliteHelper.executeQuery(buildSaveSql() + "select last_insert_rowid();") { it.getLong(1) }
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

    fun queryOne(): T = SqliteHelper.executeQuery(
        "select ${relation.columnsString} from ${relation.table} where ${buildQueryCondition()}",
        ::singleResult
    )


    private fun singleResult(resultSet: ResultSet): T {
        val instance = relation.type.createInstance()
        relation.columns.forEach { entry ->
            relation.type.memberProperties.find { it.name == entry.key }!!.apply {
                isAccessible = true
            }.javaField!!.set(instance, resultSet.getObject(entry.key, entry.value.type.java))
        }
        return instance
    }

    companion object {
        fun createTable(relation: ObjectTableRelation<*>): Boolean =
            SqliteHelper.execute(relation.createTableSql).apply {
                relation.createIndexSqlList.forEach {
                    SqliteHelper.execute(it)
                }
            }

    }

}

abstract class ObjectTableRelation<T : Any>(internal val type: KClass<T>) {

    class FieldColumnRelation(val type: KClass<*>, val column: Column)

    val columns: Map<String, FieldColumnRelation>

    val createIndexSqlList: List<String>

    internal val table: String = type.simpleName!!

    init {
        val mutableColumns = mutableMapOf<String, FieldColumnRelation>()
        val mutableIndexes = mutableListOf<String>()
        type.memberProperties.forEach {
            it.javaField!!.getAnnotation(Column::class.java)?.apply {
                mutableColumns.put(it.name, FieldColumnRelation(it.returnType.classifier.cast(), this))
            }
            it.javaField!!.getAnnotation(Index::class.java)?.apply {
                mutableIndexes += "create index if not exists idx_${it.name} on $table(${it.name});"
            }
        }
        createIndexSqlList = mutableIndexes.toList()
        columns = mutableColumns.toMap()
    }

    internal val columnsString: String = columns.toList().map { it.first }.joinToString(",")

    internal val createTableSql: String = run {
        "create table if not exists $table(${
            columns.map {
                val column = it.value.column
                "${it.key} ${column.jdbcType} ${column.extraProperties} ${if (column.isNotNull) "not null" else ""}"
            }.joinToString(",")
        });"
    }
}

data class WeaponSkin(
    @Column("varchar(100)", true, "primary key")
    val uuid: String? = null,
    @Column("varchar(100)")
    val contentTiersUuid: String? = null,
    @Column("varchar(100)")
    val themeUuid: String? = null
) : PersistenceData<WeaponSkin>() {

    override val relation: ObjectTableRelation<WeaponSkin> = WeaponSkin

    companion object : ObjectTableRelation<WeaponSkin>(WeaponSkin::class)

}

data class WeaponSkinLevel(
    @Column("varchar(100)", true, "primary key") val uuid: String? = null,
    @Column("varchar(255)") val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
    @Column("varchar(100)", true) val weaponSkinUuid: String? = null,
) : PersistenceData<WeaponSkinLevel>() {

    override val relation: ObjectTableRelation<WeaponSkinLevel> = WeaponSkinLevel

    companion object : ObjectTableRelation<WeaponSkinLevel>(WeaponSkinLevel::class)

}

data class Theme(
    @Column("varchar(100)", true, "primary key") val uuid: String? = null,
    @Column("varchar(255)") val displayIcon: String? = null,
    @Column("varchar(100)") val displayName: String? = null,
) : PersistenceData<Theme>() {

    override val relation: ObjectTableRelation<Theme> = Theme

    companion object : ObjectTableRelation<Theme>(Theme::class)

}

data class ContentTier(
    @Column("varchar(100)", true, "primary key") val uuid: String? = null,
    @Column("varchar(255)") val displayIcon: String? = null,
    @Column("varchar(100)") val displayName: String? = null,
    @Column("varchar(255)") val highlightColor: String? = null,
) : PersistenceData<ContentTier>() {

    override val relation: ObjectTableRelation<ContentTier> = ContentTier

    companion object : ObjectTableRelation<ContentTier>(ContentTier::class)

}

data class Contract(
    @Column("varchar(100)", true, "primary key") val uuid: String? = null,
    @Column("varchar(255)") val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
) : PersistenceData<Contract>() {

    override val relation: ObjectTableRelation<Contract> = Contract

    companion object : ObjectTableRelation<Contract>(Contract::class)

}

/**
 * 特别说明:
 * 通行证中的UUID不保证唯一性
 * 如:皮肤点数就可能会出现重复
 * 所以不设置为Primary Key
 */
data class ContractLevel(
    @Column("varchar(100)", true) @Index val uuid: String? = null,
    @Column("varchar(100)", true) @Index val contractUuid: String? = null,
    @Column("varchar(50)", true) val type: String,
    @Column("int") val xp: Int? = null,
    @Column("int") val amount: Int? = null,
    @Column("int") val level: Int? = null,
    @Column("boolean", true) val isFreeReward: Boolean? = null,
) : PersistenceData<ContractLevel>() {

    override val relation: ObjectTableRelation<ContractLevel> = ContractLevel

    companion object : ObjectTableRelation<ContractLevel>(ContractLevel::class)

}

data class Buddie(
    @Column("varchar(100)", true, "primary key") val uuid: String? = null,
    @Column("varchar(255)", true) val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
) : PersistenceData<Buddie>() {

    override val relation: ObjectTableRelation<Buddie> = Buddie

    companion object : ObjectTableRelation<Buddie>(Buddie::class)

}

data class BuddieLevel(
    @Column("varchar(100)", true, "primary key") val uuid: String? = null,
    @Column("varchar(255)", true) val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
    @Column("varchar(100)", true) @Index val buddieUuid: String? = null
) : PersistenceData<Buddie>() {

    override val relation: ObjectTableRelation<Buddie> = Buddie

    companion object : ObjectTableRelation<Buddie>(Buddie::class)

}

data class Currency(
    @Column("varchar(100)", true, "primary key") val uuid: String? = null,
    @Column("varchar(255)", true) val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
    @Column("varchar(255)", true) val largeIcon: String? = null
) : PersistenceData<Currency>() {

    override val relation: ObjectTableRelation<Currency> = Currency

    companion object : ObjectTableRelation<Currency>(Currency::class)

}

data class PlayCard(
    @Column("varchar(100)", true, "primary key") val uuid: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
    @Column("varchar(255)", true) val largeArt: String? = null,
    @Column("varchar(255)", true) val smallArt: String? = null,
    @Column("varchar(255)", true) val wideArt: String? = null
) : PersistenceData<PlayCard>() {

    override val relation: ObjectTableRelation<PlayCard> = PlayCard

    companion object : ObjectTableRelation<PlayCard>(PlayCard::class)

}

data class PlayTitle(
    @Column("varchar(100)", true, "primary key") val uuid: String? = null,
    @Column("varchar(100)") val displayName: String? = null,
    @Column("varchar(100)") val titleText: String? = null,
) : PersistenceData<PlayTitle>() {

    override val relation: ObjectTableRelation<PlayTitle> = PlayTitle

    companion object : ObjectTableRelation<PlayTitle>(PlayTitle::class)

}

data class Spray(
    @Column("varchar(255)") val animationGif: String? = null,
    @Column("varchar(255)") val animationPng: String? = null,
    @Column("varchar(255)", true) val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
    @Column("varchar(255)") val fullIcon: String? = null,
    @Column("varchar(255)", true) val fullTransparentIcon: String? = null,
    @Column("varchar(255)", true) val isNullSpray: Boolean? = null,
    @Column("varchar(100)", true, "primary key") val uuid: String? = null
) : PersistenceData<Spray>() {

    override val relation: ObjectTableRelation<Spray> = Spray

    companion object : ObjectTableRelation<Spray>(Spray::class)

}

data class SprayLevel(
    @Column("varchar(255)") val displayIcon: String? = null,
    @Column("varchar(100)", true) val displayName: String? = null,
    @Column("int", true) val sprayLevel: Int? = null,
    @Column("varchar(100)", true, "primary key") val uuid: String? = null,
    @Column("varchar(100)", true) @Index val sprayUuid: String? = null
) : PersistenceData<SprayLevel>() {

    override val relation: ObjectTableRelation<SprayLevel> = SprayLevel

    companion object : ObjectTableRelation<SprayLevel>(SprayLevel::class)

}

object PersistenceDataInitiator {

    private val log: MiraiLogger = MiraiLogger.Factory.create(PersistenceDataInitiator::class)


    init {
        PersistenceData.createTable(WeaponSkin)
        PersistenceData.createTable(WeaponSkinLevel)
        PersistenceData.createTable(ContentTier)
        PersistenceData.createTable(Theme)
        PersistenceData.createTable(Contract)
        PersistenceData.createTable(ContractLevel)
        PersistenceData.createTable(Buddie)
        PersistenceData.createTable(Currency)
        PersistenceData.createTable(PlayCard)
        PersistenceData.createTable(PlayTitle)
        PersistenceData.createTable(Spray)
        PersistenceData.createTable(SprayLevel)
    }


    suspend fun init() {
        withContext(Dispatchers.IO) {
            log.info("starting initial Valorant DataBase")
            initAllSkinsAndSkinLevels()
            initAllContentTiers()
            initAllThemes()
            initAllContracts()
            initAllBuddies()
            initAllCurrencies()
            initAllPlayCards()
            initAllPlayTitles()
            log.info("Valorant DataBase initialled successful")
        }
    }

    private suspend fun initAllSkinsAndSkinLevels() {
        ThirdPartyValorantApi.AllWeaponSkins.execute().data.forEach { skin ->
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
        log.info("skins and levels data initialled")
    }

    private suspend fun initAllContentTiers() {
        ThirdPartyValorantApi.AllContentTiers.execute().data.forEach { contentTier ->
            ContentTier(
                contentTier.uuid,
                contentTier.displayIcon,
                contentTier.displayName,
                contentTier.highlightColor
            ).save()
        }
        log.info("contentTiers data initialled")
    }

    private suspend fun initAllThemes() {
        ThirdPartyValorantApi.AllThemes.execute().data.forEach { theme ->
            Theme(theme.uuid, theme.displayIcon, theme.displayName).save()
        }
        log.info("themes data initialled")
    }

    private suspend fun initAllContracts() {
        ThirdPartyValorantApi.AllContracts.execute().data.forEach {
            var levelInt = 1
            it.content.apply {
                Contract(it.uuid, it.displayIcon, it.displayName).save()
            }.chapters.sortedBy { chapter -> chapter.levels.sumOf { it.xp } }
                .forEach { chapter ->
                    chapter.levels.sortedBy { level -> level.xp }.forEach { level ->
                        ContractLevel(
                            level.reward.uuid,
                            it.uuid,
                            level.reward.type,
                            level.xp,
                            level.reward.amount,
                            levelInt++,
                            false
                        ).save()
                    }
                    chapter.freeRewards?.forEach { freeReward ->
                        ContractLevel(
                            freeReward.uuid,
                            it.uuid,
                            freeReward.type,
                            null,
                            freeReward.amount,
                            null,
                            true
                        ).save()
                    }
                }
        }
        log.info("contracts and levels data initialled")
    }

    private suspend fun initAllBuddies() {
        ThirdPartyValorantApi.AllBuddies.execute().data.forEach { buddie ->
            Buddie(buddie.uuid, buddie.displayIcon, buddie.displayName).save()
            buddie.levels.forEach { level ->
                BuddieLevel(level.uuid, level.displayIcon, level.displayName, buddie.uuid).save()
            }
        }
        log.info("buddies data initialled")
    }

    private suspend fun initAllCurrencies() {
        ThirdPartyValorantApi.AllCurrencies.execute().data.forEach { currency ->
            Currency(currency.uuid, currency.displayIcon, currency.displayName, currency.largeIcon).save()
        }
        log.info("currencies data initialled")
    }

    private suspend fun initAllPlayCards() {
        ThirdPartyValorantApi.AllPlayerCards.execute().data.forEach { playCard ->
            PlayCard(playCard.uuid, playCard.displayName, playCard.largeArt, playCard.smallArt, playCard.wideArt).save()
        }
        log.info("playCards data initialled")
    }

    private suspend fun initAllPlayTitles() {
        ThirdPartyValorantApi.AllPlayerTitles.execute().data.forEach { playTitle ->
            PlayTitle(playTitle.uuid, playTitle.displayName, playTitle.titleText).save()
        }
        log.info("playTitles data initialled")
    }

}