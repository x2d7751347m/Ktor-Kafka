package com.x2d7751347m.entity

import com.x2d7751347m.dto.GlobalDto
import com.x2d7751347m.dto.UserPost
import com.x2d7751347m.plugins.BigDecimalSerializer
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.pattern
import io.r2dbc.spi.Blob
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.komapper.annotation.*
import java.math.BigDecimal

@Serializable
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

val emailStorage = mutableListOf<Email>()
val userStorage = mutableListOf<User>()
val currentMoment: Instant = Clock.System.now()
val localDateTime1 = currentMoment.toLocalDateTime(TimeZone.UTC)

val validateUserPost = Validation {
    UserPost::username ifPresent {
//        minLength(2)
        maxLength(30)
    }
    UserPost::nickname ifPresent {
        pattern("[a-z\\d_]{2,20}$") hint "Please provide a valid nickname that combination of numbers or letters.)"
        minLength(4)
        maxLength(16)

    }
    UserPost::password ifPresent {
        pattern("^(?=.*[a-zA-Z])(?=.*\\d)[A-Za-z\\d#@\$^!%*?&()\\-_=+`~\\[{\\]};:'\",<.>/]{8,30}$") hint "Please provide a valid password that combination of numbers and letters. Also, special characters are allowed.)"
//        pattern("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[#@$^!%*?&()\\-_=+`~\\[{\\]};:'\",<.>/])[A-Za-z\\d#@\$^!%*?&()\\-_=+`~\\[{\\]};:'\",<.>/]{8,20}$") hint "Please provide a valid password"
    }
    UserPost::tribal required  {
        }
}

val validateUser = Validation {
    User::username ifPresent {
//        minLength(2)
        maxLength(30)
    }
    User::nickname ifPresent {
        pattern("[a-z\\d_]{2,20}$") hint "Please provide a valid nickname that combination of numbers or letters.)"
        minLength(4)
        maxLength(16)

    }
    User::password ifPresent {
        pattern("^(?=.*[a-zA-Z])(?=.*\\d)[A-Za-z\\d#@\$^!%*?&()\\-_=+`~\\[{\\]};:'\",<.>/]{8,30}$") hint "Please provide a valid password that combination of numbers and letters. Also, special characters are allowed.)"
//        pattern("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[#@$^!%*?&()\\-_=+`~\\[{\\]};:'\",<.>/])[A-Za-z\\d#@\$^!%*?&()\\-_=+`~\\[{\\]};:'\",<.>/]{8,20}$") hint "Please provide a valid password"
    }
}

val validateEmail = Validation {
    Email::address ifPresent {
                pattern("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])") hint "Please provide a valid email.)"
    }
}

val validateEmailData = Validation {
    EmailData::address ifPresent {
        pattern("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])") hint "Please provide a valid email.)"
    }
}

val validateLoginForm = Validation {
    GlobalDto.LoginForm::username ifPresent {
//        minLength(2)
        maxLength(30)
    }
    GlobalDto.LoginForm::password ifPresent {
        pattern("^(?=.*[a-z])(?=.*\\d)[A-Za-z\\d#@\$^!%*?&()\\-_=+`~\\[{\\]};:'\",<.>/]{8,30}$") hint "Please provide a valid password that String between 8 and 30 characters that satisfies any combination of combination of numbers and letters. Also, special characters are allowed."
    }
}

@Serializable
data class NetworkPacket(
    val networkPacket: NetworkStatus,
    val user: User,
)

enum class NetworkStatus(val status: String) {
    ENTRY("entry"), EXIT("exit"), PLAYER_SYNC("playerSync"), PLAYER_ARRAY_SYNC("playerArraySync"),
}

@Serializable
data class User(
    var id: Long = 0,
    var username: String = "",
    var nickname: String = "",
    var password: String = "",
    // Highly Inflationary Currency
    @Serializable(with = BigDecimalSerializer::class)
    var credit: BigDecimal = BigDecimal.ZERO,
    var profileImageId: Long? = null,
    var tribal: Int? = null,
    var currentHead: Int? = null,
    var currentTop: Int? = null,
    var currentBottom: Int? = null,
    var currentBoost: Int? = null,
    var userRole: UserRole = UserRole.USER,
    var userStatus: UserStatus = UserStatus.ACTIVE,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val version: Int = 0,
    var departmentId: Int? = null,
    val addressId: Int? = null,
    var managerId: Long? = null,
    var hiredate: LocalDate? = null,
)

@Serializable
data class UserData(
    var id: Long? = null,
    var username: String? = null,
    var nickname: String? = null,
    var password: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    var credit: BigDecimal? = null,
    var profileImageId: Long? = null,
    var tribal: Int? = null,
    var currentHead: Int? = null,
    var currentTop: Int? = null,
    var currentBottom: Int? = null,
    var currentBoost: Int? = null,
    var userRole: UserRole? = null,
    var userStatus: UserStatus? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val version: Int? = null,
    var departmentId: Int? = null,
    val addressId: Int? = null,
    var managerId: Long? = null,
    var hiredate: LocalDate? = null,
)

enum class UserRole {
    ADMIN, USER,
}

enum class UserStatus {
    ACTIVE, SLEEP, QUIT
}

@KomapperEntityDef(User::class, ["user", "admin"])
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

@Serializable
data class Email(
    var id: Long = 0,
    var address: String = "",
    var userId: Long? = null,
    val version: Int = 0,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

@Serializable
data class EmailData(
    var id: Long? = null,
    var address: String? = null,
    var userId: Long? = null,
    val version: Int? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

@KomapperEntityDef(Email::class)
data class EmailDef(
    @KomapperId
    @KomapperAutoIncrement
    val id: Nothing,
    @KomapperVersion
    val version: Nothing,
    @KomapperCreatedAt
    val createdAt: LocalDateTime,
    @KomapperUpdatedAt
    val updatedAt: LocalDateTime,
)

@Serializable
data class ImageFile(
    var id: Long = 0,
    var name: String = "",
    var type: String = "",
    var userId: Long? = null,
    val version: Int = 0,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    var data: Blob? = null,
)

@Serializable
data class ImageFileData(
    var id: Long? = null,
    var name: String? = null,
    var type: String? = null,
    var userId: Long? = null,
    val version: Int? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    var data: Blob? = null,
)

@KomapperEntityDef(ImageFile::class)
data class ImageFileDef(
    @KomapperId
    @KomapperAutoIncrement
    val id: Nothing,
    @KomapperVersion
    val version: Nothing,
    @KomapperCreatedAt
    val createdAt: LocalDateTime,
    @KomapperUpdatedAt
    val updatedAt: LocalDateTime,
)