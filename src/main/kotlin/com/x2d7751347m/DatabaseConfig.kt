package com.x2d7751347m

import com.x2d7751347m.entity.user
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import io.r2dbc.spi.R2dbcTransientResourceException
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.r2dbc.R2dbcDatabase

private val initialR2dbcDatabase: () -> R2dbcDatabase = {
    val driver = HoconApplicationConfig(ConfigFactory.load()).property("ktor.deployment.db.driver").getString()
    val port =
        HoconApplicationConfig(ConfigFactory.load()).property("ktor.deployment.db.port").getString().toInt()
    val host = HoconApplicationConfig(ConfigFactory.load()).property("ktor.deployment.db.host").getString()
    val username =
        HoconApplicationConfig(ConfigFactory.load()).property("ktor.deployment.db.username").getString()
    val password =
        HoconApplicationConfig(ConfigFactory.load()).property("ktor.deployment.db.password").getString()
    val options = ConnectionFactoryOptions.builder()
        .option(ConnectionFactoryOptions.DRIVER, driver)
        .option(ConnectionFactoryOptions.HOST, host)
        .option(ConnectionFactoryOptions.PORT, port)
        .option(ConnectionFactoryOptions.USER, username)
        .option(ConnectionFactoryOptions.PASSWORD, password)
        .option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
        .build()
    R2dbcDatabase(options)
}

val r2dbcDatabase: () -> R2dbcDatabase = {
    val port =
        HoconApplicationConfig(ConfigFactory.load()).property("ktor.deployment.db.port").getString().toInt()
    val host = HoconApplicationConfig(ConfigFactory.load()).property("ktor.deployment.db.host").getString()
    val name = HoconApplicationConfig(ConfigFactory.load()).property("ktor.deployment.db.name").getString()
    val username =
        HoconApplicationConfig(ConfigFactory.load()).property("ktor.deployment.db.username").getString()
    val password =
        HoconApplicationConfig(ConfigFactory.load()).property("ktor.deployment.db.password").getString()
    val options = ConnectionFactoryOptions.builder()
        .option(ConnectionFactoryOptions.DRIVER, "mariadb")
        .option(ConnectionFactoryOptions.DATABASE, name)
        .option(ConnectionFactoryOptions.HOST, host)
        .option(ConnectionFactoryOptions.PORT, port)
        .option(ConnectionFactoryOptions.USER, username)
        .option(ConnectionFactoryOptions.PASSWORD, password)
        .option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
        .build()
    R2dbcDatabase(options)
}

suspend fun initR2dbcDatabase() {
    val db: R2dbcDatabase = r2dbcDatabase()
    val userDef = Meta.user
    val name = HoconApplicationConfig(ConfigFactory.load()).property("ktor.deployment.db.name").getString()
    // create a schema
    try {
        initialR2dbcDatabase().runQuery {
            QueryDsl.executeScript("CREATE DATABASE `$name` /*!40100 COLLATE 'utf8mb4_general_ci' */;")
        }
    } catch (_: R2dbcTransientResourceException){}
    db.runQuery {
        QueryDsl.create(userDef)
    }
    db.runQuery {
        QueryDsl.executeScript(
            "ALTER TABLE `user`\n" +
                    "\tCHANGE COLUMN `credit` `credit` DECIMAL(65,0) NOT NULL DEFAULT 0 AFTER `password`;"
        )
    }
}