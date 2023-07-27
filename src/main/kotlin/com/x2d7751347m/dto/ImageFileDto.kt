package com.x2d7751347m.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ImageFileResponse(
    var id: Long? = null,
    var name: String? = null,
    var type: String? = null,
    var data: ByteArray? = null,
    var userId: Long? = null,
)