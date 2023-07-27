package com.x2d7751347m.repository

import com.x2d7751347m.entity.*
import com.x2d7751347m.plugins.ConflictException
import com.x2d7751347m.r2dbcDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.operator.count
import org.komapper.core.dsl.query.firstOrNull
import org.komapper.core.dsl.query.on
import org.mindrot.jbcrypt.BCrypt
import java.math.BigDecimal

class UserRepository {

    val emailDef = Meta.email
    private val onEmailUser = on { emailDef.userId eq userDef.id }
    val userDef = Meta.user
    val adminDef = Meta.admin
    val db = r2dbcDatabase
    suspend fun insertUser(user: User): User {
        fetchUserByUsername(user.username)?.run { throw ConflictException("This username is already in use.") }
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

    suspend fun updateUser(userData: UserData): User {
        userData.username?.run { fetchUserByUsername(userData.username!!)?.run { throw ConflictException("This username is already in use.")} }
        db.runQuery {
            QueryDsl.update(userDef)
                .set {
                    userData.username?.run {

                        userDef.username eq this }
                    userData.nickname?.run { userDef.nickname eq this }
                    userData.password?.run { userDef.password eq BCrypt.hashpw(this, BCrypt.gensalt(12)) }
                    userData.credit?.run { userDef.credit eq this }
                    userData.profileImageId?.run { userDef.profileImageId eq this }
                    userData.tribal?.run { userDef.tribal eq this }
                    userData.currentTop?.run { userDef.currentTop eq this }
                    userData.currentBoost?.run { userDef.currentBoost eq this }
                    userData.currentBottom?.run { userDef.currentBottom eq this }
                    userData.currentHead?.run { userDef.currentHead eq this }
                    userData.userStatus?.run { userDef.userStatus eq this }
                }
                .where { userDef.id eq userData.id }
        }
        return db.flowQuery {
            QueryDsl.from(userDef).where { userDef.id eq userData.id }
        }.last()
    }

    suspend fun insertAdmin(admin: User): User {
        fetchUserByUsername(admin.username)?.run { throw ConflictException("This username is already in use.") }
        db.runQuery {
            QueryDsl.insert(adminDef).single(admin.apply {
                // gensalt's log_rounds parameter determines the complexity
                // the work factor is 2**log_rounds, and the default is 10
                val hashed = BCrypt.hashpw(password, BCrypt.gensalt(12))
                password = hashed
            })
        }
        return db.flowQuery {
            QueryDsl.from(adminDef).where { adminDef.username eq admin.username }
        }.last()
    }

    suspend fun fetchUsers(page: Int, size: Int): Flow<User> {

        // SELECT
        val user = db.flowQuery {
            QueryDsl.from(userDef).offset((page - 1).times(size)).limit(size)
        }
        return user
    }

    suspend fun fetchUser(id: Long): User? {

        // SELECT
        val user = db.runQuery {
            QueryDsl.from(userDef).where { userDef.id eq id }.firstOrNull()
        }
        return user
    }

    suspend fun fetchUserByUsername(username: String): User? {

        // SELECT
        val user = db.runQuery {
            QueryDsl.from(userDef).where { userDef.username eq username }.firstOrNull()
        }
        return user
    }

    suspend fun fetchUserByNickName(nickname: String): User? {

        // SELECT
        val user = db.runQuery {
            QueryDsl.from(userDef).where { userDef.nickname eq nickname }.firstOrNull()
        }
        return user
    }
    suspend fun fetchUserByEmail(address: String): User? {

        // SELECT
        val user = db.runQuery {
            QueryDsl.from(userDef).innerJoin(emailDef, onEmailUser).where { emailDef.address eq address }.firstOrNull()
        }
        return user
    }

    suspend fun deleteUser(id: Long) {
        db.runQuery { QueryDsl.delete(userDef).where { userDef.id eq id } }
    }
}