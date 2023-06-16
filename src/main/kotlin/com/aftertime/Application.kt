package com.aftertime

import io.ktor.server.application.*
import com.aftertime.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.text.get

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
