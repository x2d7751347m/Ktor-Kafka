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
    val service = Service()
    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
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

        webSocket("/chat") {
//            println("Adding user!")
            val thisConnection = Connection(this)
            connections += thisConnection
            val user = service.findUser(1)!!
            try {
                send("You are connected! There are ${connections.count()} users here.")
                send("${Json.encodeToJsonElement(NetworkPacket(NetworkStatus.ENTRY, user))}")
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
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
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
//                println("Removing $thisConnection!")
                connections -= thisConnection
            }
        }
    }
}