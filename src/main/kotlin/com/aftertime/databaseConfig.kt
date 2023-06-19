package com.aftertime

import com.aftertime.Entity.user
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.r2dbc.R2dbcDatabase

fun r2dbcDatabase(): R2dbcDatabase {
    val port =
        HoconApplicationConfig(ConfigFactory.load()).propertyOrNull("ktor.deployment.db.port")!!.getString().toInt()
    val host = HoconApplicationConfig(ConfigFactory.load()).propertyOrNull("ktor.deployment.db.host")!!.getString()
    val name = HoconApplicationConfig(ConfigFactory.load()).propertyOrNull("ktor.deployment.db.name")!!.getString()
    val username =
        HoconApplicationConfig(ConfigFactory.load()).propertyOrNull("ktor.deployment.db.username")!!.getString()
    val password =
        HoconApplicationConfig(ConfigFactory.load()).propertyOrNull("ktor.deployment.db.password")!!.getString()
    val options = ConnectionFactoryOptions.builder()
        .option(ConnectionFactoryOptions.DRIVER, "mariadb")
        .option(ConnectionFactoryOptions.DATABASE, name)
        .option(ConnectionFactoryOptions.HOST, host)
        .option(ConnectionFactoryOptions.PORT, port)
        .option(ConnectionFactoryOptions.USER, username)
        .option(ConnectionFactoryOptions.PASSWORD, password)
        .option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
        .build()
    return R2dbcDatabase(options)
}

suspend fun initR2dbcDatabase() {
    val db: R2dbcDatabase = r2dbcDatabase()
    val userDef = Meta.user
    // create a schema
    db.runQuery {
        QueryDsl.create(userDef)
    }
}