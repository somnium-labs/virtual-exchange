package com.linebizplus.exchange.mock.components

import com.linebizplus.exchange.mock.configuration.AppConfig
import com.linebizplus.exchange.mock.entities.BalanceTable
import com.linebizplus.exchange.mock.entities.MemberTable
import com.linebizplus.exchange.mock.entities.OrderTable
import com.linebizplus.exchange.mock.entities.TransactionTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseComponent {
    @KtorExperimentalAPI
    fun init() {
        Database.connect(HikariDataSource(hikariConfig()))
        println("connected")
        transaction {
            // create table if not exists
            create(OrderTable)
            create(BalanceTable)
            create(MemberTable)
            create(TransactionTable)
        }
    }
}

@KtorExperimentalAPI
private fun hikariConfig() =
    HikariConfig().apply {
        driverClassName = AppConfig.Database.driverClassName
        jdbcUrl = AppConfig.Database.jdbcUrl
        maximumPoolSize = AppConfig.Database.maximumPoolSize
        isAutoCommit = AppConfig.Database.isAutoCommit
        username = AppConfig.Database.username
        password = AppConfig.Database.password
        transactionIsolation = AppConfig.Database.transactionIsolation
        validate()
    }

suspend fun <T> query(block: () -> T): T = withContext(Dispatchers.IO) {
    transaction {
        block()
    }
}
