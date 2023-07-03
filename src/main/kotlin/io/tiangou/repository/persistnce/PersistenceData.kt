package io.tiangou.repository.persistnce

import io.tiangou.other.jdbc.SqliteHelper
import net.mamoe.mirai.console.util.cast
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
annotation class Column(val jdbcType: String, val isNotNull: Boolean = false)

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

/**
 * 主键 注解
 * 指定单主键或者复合主键
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrimaryKey

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
                if (value is CharSequence) {
                    columnsValuesJoiner.add("'$value'")
                } else {
                    columnsValuesJoiner.add("$value")
                }
            }
        }
        return "insert or ignore into ${relation.table}($columnsJoiner) values($columnsValuesJoiner);"
    }

    fun save(): Boolean {
        return SqliteHelper.execute(buildSaveSql())
    }

    fun saveAndReturnId(): Long? {
        return SqliteHelper.executeGenerateKeys(buildSaveSql())
    }

    private fun buildQueryCondition(): String {
        val queryConditionList = mutableListOf<String>()
        relation.columns.forEach { entry ->
            val field = this.javaClass.kotlin.memberProperties.find { it.name == entry.key }!!
            field.isAccessible = true
            val value = field.get(this)
            if (value != null && value.toString().isNotEmpty()) {
                if (value is CharSequence) {
                    queryConditionList.add("${entry.key} = '$value'")
                } else {
                    queryConditionList.add("${entry.key} = $value")
                }
            }
        }
        return if (queryConditionList.isEmpty()) "" else queryConditionList.joinToString(" and ")
    }

    fun queryOne(): T? = SqliteHelper.executeQuery(
        "select ${relation.columnsString} from ${relation.table} where ${buildQueryCondition()}",
        ::singleResult
    )

    fun count(): Long =
        SqliteHelper.executeQuery("select count(*) from ${relation.table} where ${buildQueryCondition()}") {
            it.getLong(1)
        }!!


    private fun singleResult(resultSet: ResultSet): T? {
        if (resultSet.next()) {
            val instance = relation.type.createInstance()
            relation.columns.forEach { entry ->
                if (resultSet.getObject(entry.key) != null) {
                    relation.type.memberProperties.find { it.name == entry.key }!!.apply {
                        isAccessible = true
                    }.javaField!!.set(instance, resultSet.getObject(entry.key, entry.value.type.java))
                }
            }
            return instance
        }
        return null
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

    data class FieldColumnRelation(val type: KClass<*>, val column: Column)

    val columns: Map<String, FieldColumnRelation>

    private val primaryKeys: List<String>

    val createIndexSqlList: List<String>

    internal val table: String = type.simpleName!!

    init {
        val mutableColumns = mutableMapOf<String, FieldColumnRelation>()
        val mutableIndexes = mutableListOf<String>()
        val mutablePrimaryKeys = mutableListOf<String>()
        type.memberProperties.forEach {
            it.javaField!!.getAnnotation(Column::class.java)?.apply {
                mutableColumns[it.name] = FieldColumnRelation(it.returnType.classifier.cast(), this)
            }
            it.javaField!!.getAnnotation(Index::class.java)?.apply {
                mutableIndexes += "create index if not exists idx_${it.name} on $table(${it.name});"
            }
            it.javaField!!.getAnnotation(PrimaryKey::class.java)?.apply {
                mutablePrimaryKeys += it.name
            }
        }
        primaryKeys = mutablePrimaryKeys.toList()
        createIndexSqlList = mutableIndexes.toList()
        columns = mutableColumns.toMap()
    }

    internal val columnsString: String = columns.toList().joinToString(",") { it.first }

    internal val createTableSql: String = run {
        "create table if not exists $table(${
            columns.map {
                val column = it.value.column
                "${it.key} ${column.jdbcType} ${if (column.isNotNull) "not null" else ""}"
            }.joinToString(",")
        }, primary key (${primaryKeys.joinToString(",")}) );"
    }
}