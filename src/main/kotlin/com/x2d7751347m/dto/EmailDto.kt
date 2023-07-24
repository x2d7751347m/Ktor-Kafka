package com.x2d7751347m.dto

import com.x2d7751347m.plugins.BigDecimalSerializer
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class EmailUserPost(

    @field:Schema(description = "Please provide the mail address.", example = "address@domail.com")
    var address: String? = null,
)

@Serializable
data class EmailUserPatch(

    @field:Schema(description = "Please provide the mail address.", example = "address@domail.com")
    var address: String? = null,
)

@Serializable
data class EmailPost(

    @field:Schema(description = "Please provide the mail address.", example = "address@domail.com")
    var address: String? = null,
    @field:Schema(description = "Please provide a user id.", example = "1")
    var userId: Long? = null,
)

@Serializable
data class EmailPatch(
    @field:Schema(description = "Please provide the mail address.", example = "address@domail.com")
    var address: String? = null,
    @field:Schema(description = "Please provide a user id.", example = "1")
    var userId: Long? = null,
)

@Serializable
data class EmailResponse(
    var id: Long? = null,
    @field:Schema(description = "the mail address.", example = "address@domail.com")
    var address: String? = null,
    @field:Schema(description = "a user id.", example = "1")
    var userId: Long = 0,
    @field:Schema(implementation = String::class, example = "2023-06-29T13:43:00.151062")
    var createdAt: LocalDateTime? = null,
    @field:Schema(implementation = String::class, example = "2023-06-29T13:43:00.151062")
    var updatedAt: LocalDateTime? = null,
)