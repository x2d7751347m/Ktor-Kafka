package com.x2d7751347m

import com.x2d7751347m.plugins.*
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>): Unit {
    runBlocking { initR2dbcDatabase() }
    return io.ktor.server.netty.EngineMain.main(args)
}

// application.conf references the main function. This annotation prevents the IDE from marking it as unused.
@Suppress("unused")
fun Application.module() {
    configureHTTP()
    configureMonitoring()
    configureAdministration()
    configureSecurity()
    configureSockets()
    configureSerialization()
    configureRouting()
}
