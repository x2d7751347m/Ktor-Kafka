package com.aftertime.Entity

import org.komapper.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class Address(
    val addressId: Int = 0,
    val street: String,
    val version: Int = 0,
)

@KomapperEntityDef(Address::class)
data class AddressDef(
    @KomapperId
    @KomapperAutoIncrement
    val addressId: Nothing,
    @KomapperVersion
    val version: Nothing,
)

data class User(
    val uid: Long = 1,
    val tribal: Int = 1,
    val currentHead: Int? = null,
    val currentTop: Int? = null,
    val currentBottom: Int? = null,
    val currentBoostNft: Int? = null,
    val employeeNo: Int? = null,
    val nickname: String? = null,
    val managerId: Long? = null,
    val hiredate: LocalDate? = null,
    val rium: BigDecimal = BigDecimal.ZERO,
    val departmentId: Int? = null,
    val addressId: Int? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val version: Int = 0,
)

@KomapperEntityDef(User::class, ["user", "manager", "admin"])
data class UserDef(
    @KomapperId
    @KomapperAutoIncrement
    val uid: Long,
    @KomapperCreatedAt
    val createdAt: LocalDateTime,
    @KomapperUpdatedAt
    val updatedAt: LocalDateTime,
    @KomapperVersion
    val version: Int,
)

data class Department(
    val departmentId: Int,
    val departmentNo: Int,
    val departmentName: String,
    val location: String,
    val version: Int,
)

@KomapperEntityDef(Department::class)
data class DepartmentDef(
    @KomapperId
    @KomapperAutoIncrement
    val departmentId: Nothing,
    @KomapperVersion
    val version: Nothing,
)