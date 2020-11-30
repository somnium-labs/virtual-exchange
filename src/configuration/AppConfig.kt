package com.linebizplus.exchange.virtual.configuration

import com.linebizplus.exchange.virtual.extensions.getBoolean
import com.linebizplus.exchange.virtual.extensions.getInt
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.util.*

@KtorExperimentalAPI
object AppConfig {
    private lateinit var appConfig: HoconApplicationConfig

    fun loadConfig(profile: String?) {
        val url = javaClass.classLoader.getResource("application.$profile.conf")
        val fileConfig = url?.let {
            println("config -> $url")
            ConfigFactory.parseURL(url)
        } ?: ConfigFactory.load()
        appConfig = HoconApplicationConfig(fileConfig)
    }

    object Database {
        private val host = appConfig.property("ktor.database.host").getString()
        private val port = appConfig.property("ktor.database.port").getInt()
        private val schema = appConfig.property("ktor.database.schema").getString()
        private val properties = appConfig.property("ktor.database.properties").getList()

        val jdbcUrl = "jdbc:mysql://$host:$port/$schema?${properties.joinToString("&") { it }}"
        val driverClassName = appConfig.property("ktor.database.driverClassName").getString()
        val maximumPoolSize = appConfig.property("ktor.database.maximumPoolSize").getInt()
        val isAutoCommit = appConfig.property("ktor.database.isAutoCommit").getBoolean()
        val username = appConfig.property("ktor.database.username").getString()
        val password = appConfig.property("ktor.database.password").getString()
        val transactionIsolation = appConfig.property("ktor.database.transactionIsolation").getString()
    }

    object Service {
        val pairs = appConfig.property("ktor.service.pairs").getList()
    }


}

