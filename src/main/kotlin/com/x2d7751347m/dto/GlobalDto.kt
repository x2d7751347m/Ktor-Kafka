package com.x2d7751347m.dto

import kotlinx.serialization.Serializable

class GlobalDto {

    @Serializable
    data class LoginForm(
        val username: String,
        val password: String,
        val deviceId: String,
    )
}