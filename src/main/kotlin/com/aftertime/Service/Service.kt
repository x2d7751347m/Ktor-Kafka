package com.aftertime.Service

import com.aftertime.Entity.User
import com.aftertime.Entity.user
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.first
import org.komapper.r2dbc.R2dbcDatabase

class Service {
    suspend fun a(url: String, port: Int, name: String, username: String, password: String): Long {
        val options = ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "mariadb")
            .option(ConnectionFactoryOptions.DATABASE, name)
            .option(ConnectionFactoryOptions.HOST, url)
            .option(ConnectionFactoryOptions.PORT, port)
            .option(ConnectionFactoryOptions.USER, username)
            .option(ConnectionFactoryOptions.PASSWORD, password)
            .option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
            .build()
        val db: R2dbcDatabase = R2dbcDatabase(options)
        // get a metamodel
        val a = Meta.user
        // execute simple CRUD operations in a transaction
        db.withTransaction {
            // create a schema
            db.runQuery {
                QueryDsl.create(a)
            }

            // INSERT
            val newUser = db.runQuery {
                QueryDsl.insert(a).single(User(nickname = "aaaa"))
            }

            // SELECT
            val nickname = db.runQuery {
                QueryDsl.from(a).where { a.nickname eq newUser.nickname }.first()
            }
        }

        return db.runQuery {
            QueryDsl.from(a).where { a.nickname eq "aaaa" }
        }.last().uid
    }
}