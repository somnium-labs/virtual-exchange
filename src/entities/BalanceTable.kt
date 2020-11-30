package com.linebizplus.exchange.mock.entities

import org.jetbrains.exposed.sql.Table

object BalanceTable : Table("balance") {
    private val id = long("id").autoIncrement()
    val memberId = (long("member_id") references MemberTable.id)
    val asset = varchar("asset", 5)
    val amount = decimal("amount", 20, 8)
    val locked = decimal("locked", 20, 8)

    init {
        index(true, memberId, asset)
    }

    override val primaryKey = PrimaryKey(id)
}
