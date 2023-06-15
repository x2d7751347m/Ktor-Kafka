package com.aftertime

import io.ktor.server.application.*
import com.aftertime.plugins.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureAdministration()
    configureSockets()
    configureSerialization()
    configureRouting()
}
