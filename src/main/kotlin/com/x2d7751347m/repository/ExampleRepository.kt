package com.x2d7751347m.repository

import com.x2d7751347m.entity.*
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.operator.count
import org.komapper.core.dsl.operator.plus
import org.komapper.core.dsl.query.firstOrNull
import org.komapper.core.dsl.query.on
import org.komapper.core.dsl.query.where
import org.komapper.r2dbc.R2dbcDatabase
import java.math.BigDecimal

class ExampleRepository(private val db: R2dbcDatabase) {

    private val d = Meta.department
    private val u = Meta.user
    private val ad = Meta.admin
    private val a = Meta.address
    private val onUserDepartment = on { u.departmentId eq d.departmentId }
    private val onUserManager = on { u.managerId eq ad.id }
    private val onUserAddress = on { u.addressId eq a.addressId }
    private val isHighPerformer = where { u.credit greaterEq BigDecimal(3_000) }

    suspend fun fetchUserById(id: Long): User? {
        val query = QueryDsl.from(u).where { u.id eq id }.firstOrNull()
        return db.runQuery(query)
    }

    suspend fun fetchAddressById(addressId: Int): Address? {
        val query = QueryDsl.from(a).where { a.addressId eq addressId }.firstOrNull()
        return db.runQuery(query)
    }

    suspend fun fetchHighPerformers(): List<User> {
        val query = QueryDsl.from(u).where(isHighPerformer).orderBy(u.id)
        return db.runQuery(query)
    }

    suspend fun fetchDepartmentsContainingAnyHighPerformers(): List<Department> {
        val subquery = QueryDsl.from(u).where(isHighPerformer).select(u.departmentId)
        val query = QueryDsl.from(d).where {
            d.departmentId inList { subquery }
        }.orderBy(d.departmentId)
        return db.runQuery(query)
    }

    suspend fun fetchAllUsers(): List<User> {
        val query = QueryDsl.from(u).orderBy(u.id)
        return db.runQuery(query)
    }

    suspend fun fetchUsers(credit: BigDecimal? = null, departmentName: String? = null): List<User> {
        val query = QueryDsl.from(u)
            .innerJoin(d, onUserDepartment)
            .where {
                u.credit eq credit
                d.departmentName eq departmentName
            }.orderBy(u.id)
        return db.runQuery(query)
    }

    suspend fun fetchDepartmentNameAndUserSize(): List<Pair<String?, Long?>> {
        val query = QueryDsl.from(d)
            .leftJoin(u, onUserDepartment)
            .orderBy(d.departmentId)
            .groupBy(d.departmentName)
            .select(d.departmentName, count(u.id))
        return db.runQuery(query)
    }

    suspend fun fetchDepartmentUsers(): Map<Department, Set<User>> {
        val query = QueryDsl.from(d)
            .leftJoin(u, onUserDepartment)
            .orderBy(d.departmentId)
            .includeAll()
        val store = db.runQuery(query)
        return store.oneToMany(d, u)
    }

    suspend fun fetchManagerUsers(): Map<User, Set<User>> {
        val query = QueryDsl.from(u)
            .leftJoin(ad, onUserManager)
            .orderBy(u.managerId)
            .includeAll()
        val store = db.runQuery(query)
        return store.oneToMany(ad, u)
    }

    suspend fun fetchUserAddress(): Map<User, Address?> {
        val query = QueryDsl.from(u)
            .leftJoin(a, onUserAddress)
            .orderBy(u.id)
            .includeAll()
        val store = db.runQuery(query)
        return store.oneToOne(u, a)
    }

    suspend fun fetchAllAssociations(): Triple<Map<Department, Set<User>>, Map<User, Address?>, Map<User, Set<User>>> {
        val query = QueryDsl.from(d)
            .leftJoin(u, onUserDepartment)
            .leftJoin(a, onUserAddress)
            .leftJoin(ad, onUserManager)
            .orderBy(d.departmentId)
            .includeAll()
        val store = db.runQuery(query)
        val deptUsr = store.oneToMany(d, u)
        val usrAddr = store.oneToOne(u, a)
        val mgrUsr = store.oneToMany(ad, u)
        return Triple(deptUsr, usrAddr, mgrUsr)
    }

    suspend fun updateUser(user: User): User {
        val query = QueryDsl.update(u).single(user)
        return db.runQuery(query)
    }

    suspend fun updateCreditOfHighPerformers(raise: BigDecimal): Long {
        val query = QueryDsl.update(u).set {
            u.credit eq u.credit + raise
        }.where(isHighPerformer)
        return db.runQuery(query)
    }

    suspend fun insertAddress(address: Address): Address {
        val query = QueryDsl.insert(a).single(address)
        return db.runQuery(query)
    }

    suspend fun upsertAddress(address: Address): Address {
        val query = QueryDsl.insert(a).onDuplicateKeyUpdate().executeAndGet(address)
        return db.runQuery(query)
    }
}