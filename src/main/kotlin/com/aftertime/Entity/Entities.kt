package com.aftertime.Entity

import com.aftertime.plugins.BigDecimalSerializer
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.pattern
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.komapper.annotation.*
import java.math.BigDecimal

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

val userStorage = mutableListOf<User>()
val currentMoment: Instant = Clock.System.now()
val localDateTime1 = currentMoment.toLocalDateTime(TimeZone.UTC)

val validateUser = Validation {
    User::nickname ifPresent {
        minLength(2)
        maxLength(20)
    }
    User::password ifPresent {
        pattern("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[#@$^!%*?&()\\-_=+`~\\[{\\]};:'\",<.>/])[A-Za-z\\d#@\$^!%*?&()\\-_=+`~\\[{\\]};:'\",<.>/]{8,20}$") hint "Please provide a valid email address (optional)"
    }
}

@Serializable
data class User(
    val id: Long = 1,
    val nickname: String = "",
    val password: String = "",
    val tribal: Int = 1,
    val currentHead: Int? = null,
    val currentTop: Int? = null,
    val currentBottom: Int? = null,
    val currentBoostNft: Int? = null,
    val employeeNo: Int? = null,
    val managerId: Long? = null,
    val hiredate: LocalDate? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val rium: BigDecimal = BigDecimal.ZERO,
    val departmentId: Int? = null,
    val addressId: Int? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val version: Int = 0,
)

@KomapperEntityDef(User::class, ["user", "manager", "admin"])
data class UserDef(
    @KomapperId
    @KomapperAutoIncrement
    val id: Long,
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