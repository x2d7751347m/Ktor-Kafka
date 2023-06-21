package com.aftertime.Service

import com.aftertime.Entity.User
import com.aftertime.Entity.admin
import com.aftertime.Entity.user
import com.aftertime.r2dbcDatabase
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.firstOrNull
import org.mindrot.jbcrypt.BCrypt

class Service {
    suspend fun createUser(user: User): User {
        val userDef = Meta.user
        val db = r2dbcDatabase()
        db.runQuery {
            QueryDsl.insert(userDef).single(user.apply {
                // gensalt's log_rounds parameter determines the complexity
                // the work factor is 2**log_rounds, and the default is 10
                val hashed = BCrypt.hashpw(password, BCrypt.gensalt(12))

                // Check that an unencrypted password matches one that has
                // previously been hashed
                if (BCrypt.checkpw(password, hashed)) println("It matches") else println("It does not match")
                password = hashed
            })
        }
        return db.runQuery {
            QueryDsl.from(userDef).where { userDef.nickname eq user.nickname }
        }.last()
    }

    suspend fun createAdmin(admin: User): User {
        val adminDef = Meta.admin
        val db = r2dbcDatabase()
        db.runQuery {
            QueryDsl.insert(adminDef).single(admin.apply {
                // gensalt's log_rounds parameter determines the complexity
                // the work factor is 2**log_rounds, and the default is 10
                val hashed = BCrypt.hashpw(password, BCrypt.gensalt(12))
                password = hashed
            })
        }
        return db.runQuery {
            QueryDsl.from(adminDef).where { adminDef.nickname eq admin.nickname }
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

    suspend fun findUserByUsername(username: String): User? {

        val userDef = Meta.user
        val db = r2dbcDatabase()

        // SELECT
        val user = db.runQuery {
            QueryDsl.from(userDef).where { userDef.username eq username }.firstOrNull()
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

    suspend fun findUserByNickName02(nickname: String): User? {

        val userDef = Meta.user
        val db = r2dbcDatabase()

        // SELECT
        val user = db.runQuery {
            QueryDsl.from(userDef).where { userDef.nickname eq nickname }.firstOrNull()
        }
        return user
    }
}