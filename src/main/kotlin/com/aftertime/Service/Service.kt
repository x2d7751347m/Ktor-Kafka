package com.aftertime.Service

import com.aftertime.Entity.Address
import com.aftertime.Entity.address
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.first
import org.komapper.r2dbc.R2dbcDatabase
import java.util.Properties

class Service {
    suspend fun a(): Int {
        val b = Properties()
        println(b.isEmpty)
        println(Properties().getProperty("aaa.config"))
        // create a Database instance
        val options = ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "h2")
            .option(ConnectionFactoryOptions.PROTOCOL, "mem")
            .option(ConnectionFactoryOptions.DATABASE, "pararium")
            .option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
            .build()
        val db: R2dbcDatabase = R2dbcDatabase(options)
        // get a metamodel
        val a = Meta.address
            // execute simple CRUD operations in a transaction
            db.withTransaction {
                // create a schema
                db.runQuery {
                    QueryDsl.create(a)
                }

                // INSERT
                val newAddress = db.runQuery {
                    QueryDsl.insert(a).single(Address(street = "street A"))
                }

                // SELECT
                val address1 = db.runQuery {
                    QueryDsl.from(a).where { a.addressId eq newAddress.addressId }.first()
                }
            }
        return db.runQuery {
            QueryDsl.from(a).where { a.street eq "street A" }
        }.last().addressId
    }
}