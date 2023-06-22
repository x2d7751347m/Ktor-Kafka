package com.aftertime

import io.ktor.websocket.*
import java.util.concurrent.atomic.*

class Connection(val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicLong(0)
    }
    val name = lastId.getAndIncrement()
//        "user${lastId.getAndIncrement()}"
}