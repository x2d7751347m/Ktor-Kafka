package com.x2d7751347m.dto

import com.x2d7751347m.entity.UserStatus
import com.x2d7751347m.plugins.BigDecimalSerializer
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class UserPost(

    var username: String? = null,
    var nickname: String? = null,
    var password: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @field:Schema(description = "Highly Inflationary Currency", implementation = String::class, type = "string", example = "0")
    var credit: BigDecimal = BigDecimal.ZERO,
)

@Serializable
data class UserPatch(
    var username: String? = null,
    var nickname: String? = null,
    var password: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @field:Schema(description = "Highly Inflationary Currency", implementation = String::class, type = "string", example = "0")
    var credit: BigDecimal = BigDecimal.ZERO,
)

@Serializable
data class UserResponse(
    var id: Long? = null,
    var username: String? = null,
    var nickname: String? = null,
    var password: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @field:Schema(description = "Highly Inflationary Currency", implementation = String::class, type = "string", example = "0")
    var credit: BigDecimal? = null,
    var userStatus: UserStatus? = null,
    @field:Schema(implementation = String::class, example = "2023-06-29T13:43:00.151062")
    var createdAt: LocalDateTime? = null,
    @field:Schema(implementation = String::class, example = "2023-06-29T13:43:00.151062")
    var updatedAt: LocalDateTime? = null,
)