package com.x2d7751347m.repository

import com.x2d7751347m.entity.Email
import com.x2d7751347m.entity.EmailData
import com.x2d7751347m.entity.email
import com.x2d7751347m.plugins.ConflictException
import com.x2d7751347m.options
import com.x2d7751347m.r2dbcDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.firstOrNull

class EmailRepository {

    val emailDef = Meta.email
    val db = r2dbcDatabase
    suspend fun insertEmail(email: Email): Email {
        fetchEmailByaddress(email.address)?.run { throw ConflictException("This address is already in use.") }
        db.runQuery {
            QueryDsl.insert(emailDef).single(email)
        }
        return db.runQuery {
            QueryDsl.from(emailDef).where { emailDef.address eq email.address }
        }.last()
    }

    suspend fun updateEmail(emailData: EmailData): Email {
        emailData.address?.run { fetchEmailByaddress(emailData.address!!)?.run { throw ConflictException("This address is already in use.")} }
        db.runQuery {
            QueryDsl.update(emailDef)
                .set {
                    emailData.address?.run {
                        emailDef.address eq this }
                    emailData.userId?.run { emailDef.userId eq this }
                }
                .where { emailDef.id eq emailData.id }
        }
        return db.flowQuery {
            QueryDsl.from(emailDef).where { emailDef.id eq emailData.id }
        }.last()
    }

    suspend fun fetchEmails(page: Int, size: Int): Flow<Email> {

        // SELECT
        val email = db.flowQuery {
            QueryDsl.from(emailDef).offset((page - 1).times(size)).limit(size)
        }
        return email
    }

    suspend fun fetchEmailsByUserId(page: Int, size: Int, userId: Long): Flow<Email> {

        // SELECT
        val email = db.flowQuery {
            QueryDsl.from(emailDef).where { emailDef.userId eq userId }.offset((page - 1).times(size)).limit(size)
        }
        return email
    }

    suspend fun fetchEmail(id: Long): Email? {

        // SELECT
        val email = db.runQuery {
            QueryDsl.from(emailDef).where { emailDef.id eq id }.firstOrNull()
        }
        return email
    }

    suspend fun fetchEmailByaddress(address: String): Email? {

        // SELECT
        val email = db.runQuery {
            QueryDsl.from(emailDef).where { emailDef.address eq address }.firstOrNull()
        }
        return email
    }

    suspend fun fetchAllEmailsByUserId(userId: Long): List<Email> {

        // SELECT
        val emailList = db.runQuery {
            QueryDsl.from(emailDef).where { emailDef.userId eq userId }
        }
        return emailList
    }

    suspend fun deleteEmail(id: Long) {
        db.runQuery { QueryDsl.delete(emailDef).where { emailDef.id eq id } }
    }

    suspend fun deleteAllEmailsByUserId(userId: Long) {
        db.runQuery { QueryDsl.delete(emailDef).where { emailDef.userId eq userId } }
    }
}