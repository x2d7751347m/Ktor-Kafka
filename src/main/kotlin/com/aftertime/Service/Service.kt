package com.aftertime.Service

import com.aftertime.Entity.User
import com.aftertime.Entity.user
import com.aftertime.r2dbcDatabase
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.first
import org.komapper.r2dbc.R2dbcDatabase

class Service {
    suspend fun createUser(user: User): User {
        val userDef = Meta.user
        val db = r2dbcDatabase()
        db.runQuery {
            QueryDsl.insert(userDef).single(user)
        }
        return db.runQuery {
            QueryDsl.from(userDef).where { userDef.nickname eq user.nickname }
        }.last()
    }

    suspend fun exampleService(db: R2dbcDatabase): Long {
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
        }.last().id
    }
}