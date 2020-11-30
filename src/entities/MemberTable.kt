package com.linebizplus.exchange.mock.entities

import org.jetbrains.exposed.sql.Table

object MemberTable : Table("members") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255).uniqueIndex()
    val apiKey = varchar("api_key", 64).uniqueIndex().nullable()

    override val primaryKey = PrimaryKey(id)
}
