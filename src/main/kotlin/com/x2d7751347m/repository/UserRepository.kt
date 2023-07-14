package com.x2d7751347m.repository

import com.x2d7751347m.entity.User
import com.x2d7751347m.entity.UserData
import com.x2d7751347m.entity.admin
import com.x2d7751347m.entity.user
import com.x2d7751347m.r2dbcDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.firstOrNull
import org.mindrot.jbcrypt.BCrypt

class UserRepository {

    val userDef = Meta.user
    val adminDef = Meta.admin
    val db = r2dbcDatabase()
    suspend fun createUser(user: User): User {
        db.runQuery {
            QueryDsl.insert(userDef).single(user.apply {
                password.let {
                    // gensalt's log_rounds parameter determines the complexity
                    // the work factor is 2**log_rounds, and the default is 10
                    val hashed = BCrypt.hashpw(password, BCrypt.gensalt(12))
                    password = hashed
                }
            })
        }
        return db.runQuery {
            QueryDsl.from(userDef).where { userDef.username eq user.username }
        }.last()
    }

    suspend fun patchUser(userData: UserData): User {
//        val json = Json {
//            ignoreUnknownKeys = true
//        }
//        val encodedUser = json.encodeToString(UserData.serializer(), userData)
//        val user = json.decodeFromString<User>(encodedUser)
        db.runQuery {
            QueryDsl.update(userDef)
                .set {
                    userData.username?.run { userDef.username eq this }
                    userData.nickname?.run { userDef.nickname eq this }
                    userData.password?.run { userDef.password eq BCrypt.hashpw(this, BCrypt.gensalt(12)) }
                    userData.tribe?.run { userDef.tribe eq this }
                    userData.currentHead?.run { userDef.currentHead eq this }
                    userData.currentTop?.run { userDef.currentTop eq this }
                    userData.currentBottom?.run { userDef.currentBottom eq this }
                    userData.currentBoostNft?.run { userDef.currentBoostNft eq this }
                    userData.rium?.run { userDef.rium eq this }
                    userData.userStatus?.run { userDef.userStatus eq this }
                }
                .where { userDef.id eq userData.id }
        }
        return db.flowQuery {
            QueryDsl.from(userDef).where { userDef.id eq userData.id }
        }.last()
    }

    suspend fun createAdmin(admin: User): User {
        db.runQuery {
            QueryDsl.insert(adminDef).single(admin.apply {
                // gensalt's log_rounds parameter determines the complexity
                // the work factor is 2**log_rounds, and the default is 10
                val hashed = BCrypt.hashpw(password, BCrypt.gensalt(12))
                password = hashed
            })
        }
        return db.flowQuery {
            QueryDsl.from(adminDef).where { adminDef.nickname eq admin.nickname }
        }.last()
    }

    suspend fun findUsers(page: Int, size: Int): Flow<User> {

        // SELECT
        val user = db.flowQuery {
            QueryDsl.from(userDef).offset((page - 1).times(size)).limit(size)
        }
        return user
    }

    suspend fun findUser(id: Long): User? {

        // SELECT
        val user = db.runQuery {
            QueryDsl.from(userDef).where { userDef.id eq id }.firstOrNull()
        }
        return user
    }

    suspend fun findUserByUsername(username: String): User? {

        // SELECT
        val user = db.runQuery {
            QueryDsl.from(userDef).where { userDef.username eq username }.firstOrNull()
        }
        return user
    }

    suspend fun findUserByNickName(nickname: String): User? {

        // SELECT
        val user = db.runQuery {
            QueryDsl.from(userDef).where { userDef.nickname eq nickname }.firstOrNull()
        }
        return user
    }

    suspend fun deleteUser(id: Long) {
        db.runQuery { QueryDsl.delete(userDef).where { userDef.id eq id } }
    }
}