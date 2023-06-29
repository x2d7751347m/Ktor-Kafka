package com.aftertime.dto

import com.aftertime.entity.UserStatus
import com.aftertime.plugins.BigDecimalSerializer
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class UserPost(

    var username: String? = null,
    var nickname: String? = null,
    var password: String? = null,
    @field:Schema(description = "Tribe is sometimes used to refer to a group of people of the same race, language, and customs, especially in a developing country. Some people disapprove of this use")
    var tribe: Int? = null,
    var currentHead: Int? = null,
    var currentTop: Int? = null,
    var currentBottom: Int? = null,
    var currentBoostNft: Int? = null,
)

@Serializable
data class UserPatch(
    var username: String? = null,
    var nickname: String? = null,
    var password: String? = null,
    @field:Schema(description = "Tribe is sometimes used to refer to a group of people of the same race, language, and customs, especially in a developing country. Some people disapprove of this use")
    var tribe: Int? = null,
    var currentHead: Int? = null,
    var currentTop: Int? = null,
    var currentBottom: Int? = null,
    var currentBoostNft: Int? = null,
)

@Serializable
data class UserResponse(
    var id: Long? = null,
    var username: String? = null,
    var nickname: String? = null,
    var password: String? = null,
    @field:Schema(description = "Tribe is sometimes used to refer to a group of people of the same race, language, and customs, especially in a developing country. Some people disapprove of this use")
    var tribe: Int? = null,
    var currentHead: Int? = null,
    var currentTop: Int? = null,
    var currentBottom: Int? = null,
    var currentBoostNft: Int? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @field:Schema(implementation = String::class, type = "string", example = "0")
    var rium: BigDecimal? = null,
    var userStatus: UserStatus? = null,
    @field:Schema(implementation = String::class, example = "2023-06-29T13:43:00.151062")
    var createdAt: LocalDateTime? = null,
    @field:Schema(implementation = String::class, example = "2023-06-29T13:43:00.151062")
    var updatedAt: LocalDateTime? = null,
)