package com.aftertime.Service

import com.aftertime.Entity.User
import com.aftertime.Entity.user
import com.aftertime.r2dbcDatabase
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.firstOrNull

class Service {
    suspend fun createUser(user: User): User {
        val userDef = Meta.user
        val db = r2dbcDatabase()
        db.runQuery {
            QueryDsl.insert(userDef).single(user.apply { this.password = this.password })
        }
        return db.runQuery {
            QueryDsl.from(userDef).where { userDef.nickname eq user.nickname }
        }.last()
    }

    suspend fun findUsers(page: Int, size: Int): List<User> {

        val userDef = Meta.user
        val db = r2dbcDatabase()

        // SELECT
        val user = db.runQuery {
            QueryDsl.from(userDef).offset((page - 1).times(size)).limit(size)
        }
        return user
    }

    suspend fun findUser(uid: Long): User? {

        val userDef = Meta.user
        val db = r2dbcDatabase()

        // SELECT
        val user = db.runQuery {
            QueryDsl.from(userDef).where { userDef.id eq uid }.firstOrNull()
        }
        return user
    }

    suspend fun findUserByNickName(nickname: String): User? {

        val userDef = Meta.user
        val db = r2dbcDatabase()

        // SELECT
        val user = db.runQuery {
            QueryDsl.from(userDef).where { userDef.nickname eq nickname }.firstOrNull()
        }
        return user
    }
}