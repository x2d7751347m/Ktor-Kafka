package com.aftertime.dto

import kotlinx.serialization.Serializable

class GlobalDto {

    @Serializable
    data class LoginForm(
        val username: String,
        val password: String,
    )
}