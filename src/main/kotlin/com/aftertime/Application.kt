package com.aftertime

import com.aftertime.plugins.*
import io.ktor.server.application.*

suspend fun main(args: Array<String>): Unit {
    initR2dbcDatabase()
    return io.ktor.server.netty.EngineMain.main(args)
}

// application.conf references the main function. This annotation prevents the IDE from marking it as unused.
@Suppress("unused")
fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureAdministration()
    configureSockets()
    configureSerialization()
    configureRouting()
}
