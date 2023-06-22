package com.aftertime.plugins

import com.aftertime.Connection
import com.aftertime.Entity.NetworkPacket
import com.aftertime.Entity.NetworkStatus
import com.aftertime.Service.Service
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.time.Duration
import java.util.*

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}

fun Route.socketRouting() {
    val service = Service()
    val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet(1100))
    webSocket("/ws") { // websocketSession
        send("You are connected!")
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                outgoing.send(Frame.Text("YOU SAID: $text"))
                if (text.equals("bye", ignoreCase = true)) {
                    close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                }
            }
        }
    }

//    authenticate("auth-jwt") {
    webSocket("/chat") {
        val thisConnection = Connection(this)
//        coroutineScope {

//            val principal = call.principal<JWTPrincipal>()
//            val id = principal!!.payload.getClaim("id").asLong()
//            val user = service.findUser(id)!!
//            println("Adding user!")
        connections += thisConnection
        val user = service.findUser(1)!!
        try {
            send("You are connected! There are ${connections.count()} users here.")
            send("${Json.encodeToJsonElement(NetworkPacket(NetworkStatus.ENTRY, user))}")
            connections.forEach {
                it.session.send("${thisConnection.name} is connected! There are ${connections.count()} users here.")
            }
            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> {
                        val receivedByteArray = frame.readBytes()

                        connections.forEach {
                            it.session.send(receivedByteArray)
                        }
                    }

                    is Frame.Text -> {
                        val receivedText = "${frame.readText()}"
                        val textWithUsername = "[${thisConnection.name}]: $receivedText"

                        if (receivedText.startsWith("{") && Json.decodeFromJsonElement<NetworkPacket>(
                                Json.decodeFromString(
                                    receivedText
                                )
                            ).networkPacket == NetworkStatus.EXIT
                        ) {
                            connections.forEach {
                                it.session.send("${Json.encodeToJsonElement(NetworkPacket(NetworkStatus.EXIT, user))}")
                                it.session.send("Removing ${thisConnection.name} user! ")
                            }
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        }
                        connections.forEach {
                            it.session.send(receivedText)
                        }
                    }

                    else -> {
                        throw BadRequestException("bad message format")
                    }
                }
            }
        } catch (e: Exception) {
            println(e.localizedMessage)
        } finally {
//                println("Removing $thisConnection!")
            connections -= thisConnection
        }
    }
}