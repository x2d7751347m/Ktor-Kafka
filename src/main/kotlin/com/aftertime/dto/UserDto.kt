package com.aftertime.dto

import com.aftertime.entity.UserStatus
import com.aftertime.plugins.BigDecimalSerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class UserPost(
    var username: String? = null,
    var nickname: String? = null,
    var password: String? = null,
    var tribal: Int? = null,
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
    var tribal: Int? = null,
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
    var tribal: Int? = null,
    var currentHead: Int? = null,
    var currentTop: Int? = null,
    var currentBottom: Int? = null,
    var currentBoostNft: Int? = null,
    @Serializable(with = BigDecimalSerializer::class)
    var rium: BigDecimal? = null,
    var userStatus: UserStatus? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)