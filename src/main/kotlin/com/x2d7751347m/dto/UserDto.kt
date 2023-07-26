package com.x2d7751347m.dto

import com.x2d7751347m.entity.Tribal
import com.x2d7751347m.entity.UserStatus
import com.x2d7751347m.plugins.BigDecimalSerializer
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class UserPost(

    @field:Schema(description = "The 1 to 30 lengths username that combination of numbers or letters. When logging in, this parameter is required.", example = "username")
    var username: String? = null,
    @field:Schema(description = "The 2 to 20 lengths nickname that combination of numbers or letters.", example = "username")
    var nickname: String? = null,
    @field:Schema(description = "Please provide a valid 8 to 30 lengths password that combination of numbers and letters. Also, special characters are allowed.", example = "Password1!")
    var password: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @field:Schema(description = "Highly Inflationary Currency", implementation = String::class, type = "string", example = "0")
    var credit: BigDecimal = BigDecimal.ZERO,
    var tribal: Tribal? = null,
    var currentHead: Int? = null,
    var currentTop: Int? = null,
    var currentBottom: Int? = null,
    var currentBoost: Int? = null,
)

@Serializable
data class UserPatch(
    @field:Schema(description = "The 1 to 30 lengths username that combination of numbers or letters. When logging in, this parameter is required.", example = "username")
    var username: String? = null,
    @field:Schema(description = "The 2 to 20 lengths nickname that combination of numbers or letters.", example = "username")
    var nickname: String? = null,
    @field:Schema(description = "Please provide a valid 8 to 30 lengths password that combination of numbers and letters. Also, special characters are allowed.", example = "Password1!")
    var password: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @field:Schema(description = "Highly Inflationary Currency", implementation = String::class, type = "string", example = "0")
    var credit: BigDecimal = BigDecimal.ZERO,
    var tribal: Tribal? = null,
    var currentHead: Int? = null,
    var currentTop: Int? = null,
    var currentBottom: Int? = null,
    var currentBoost: Int? = null,
)

@Serializable
data class UserResponse(
    var id: Long? = null,
    @field:Schema(description = "The 1 to 30 lengths username that combination of numbers or letters. When logging in, this parameter is required.", example = "username")
    var username: String? = null,
    @field:Schema(description = "The 2 to 20 lengths nickname that combination of numbers or letters.", example = "username")
    var nickname: String? = null,
    @field:Schema(description = "Just a password.", example = "Password1!")
    var password: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @field:Schema(description = "Highly Inflationary Currency", implementation = String::class, type = "string", example = "0")
    var credit: BigDecimal? = null,
    var tribal: Tribal? = null,
    var currentHead: Int? = null,
    var currentTop: Int? = null,
    var currentBottom: Int? = null,
    var currentBoost: Int? = null,
    var userStatus: UserStatus? = null,
    @field:Schema(implementation = String::class, example = "2023-06-29T13:43:00.151062")
    var createdAt: LocalDateTime? = null,
    @field:Schema(implementation = String::class, example = "2023-06-29T13:43:00.151062")
    var updatedAt: LocalDateTime? = null,
)