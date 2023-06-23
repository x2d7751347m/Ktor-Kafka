package com.aftertime.plugins

import com.aftertime.Connection
import com.aftertime.Entity.NetworkPacket
import com.aftertime.Entity.NetworkStatus
import com.aftertime.Entity.User
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.r2dbc.spi.R2dbcNonTransientResourceException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.io.IOException
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
    val sendCancelledChannelErrorHandler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
//    var connectionAvailability: Boolean
//    synchronized(this) {
//        connectionAvailability = true
//    }
//    val service = Service()
    val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet(200))
    val connectionMutex = Mutex()
    suspend fun <Connection> MutableSet<Connection>.addConnection(connection: Connection) {
        while (true) {
            connectionMutex.withLock {
                if (this.contains(connection)) return
                this += connection
            }
            break
        }
    }

    suspend fun <Connection> MutableSet<Connection>.removeConnection(connection: Connection) {
        while (true) {
            connectionMutex.withLock {
                if (!this.contains(connection)) return
                this -= connection
            }
            break
        }
    }
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
        val thisConnection: Connection
        connectionMutex.withLock {
            thisConnection = Connection(this)
        }
//            val principal = call.principal<JWTPrincipal>()
//            val id = principal!!.payload.getClaim("id").asLong()
//            val user = service.findUser(id)!!
//            println("Adding user!")
        connections.addConnection(thisConnection)
        try {
            val user =
//                        service.findUser(1)!!
                User()
                    .apply { id = thisConnection.name }
            send("You are connected! There are ${connections.count()} users here.")
            send("${Json.encodeToJsonElement(NetworkPacket(NetworkStatus.ENTRY, user))}")
            var connectionsCopy = synchronized(connections) { connections.toSet() }
            coroutineScope {
                connectionsCopy.forEach {
                    launch (sendCancelledChannelErrorHandler) {
                        try {
                            it.session.send("${thisConnection.name} is connected! There are ${connections.count()} users here.")
                        } catch (e: Exception) {
                            connections.removeConnection(thisConnection)
                        }
                    }
                }
            }
            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> {
                        val receivedByteArray = frame.readBytes()

                        connectionsCopy = synchronized(connections) { connections.toSet() }
                        coroutineScope {
                            connectionsCopy.forEach {
                                launch(sendCancelledChannelErrorHandler) {
                                    try {
                                        it.session.send(receivedByteArray)
                                    } catch (e: Exception) {
                                        connections.removeConnection(thisConnection)
                                    }
                                }
                            }
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
                            connectionsCopy = synchronized(connections) { connections.toSet() }
                            coroutineScope {
                                connectionsCopy.forEach {
                                    launch(sendCancelledChannelErrorHandler) {
                                        try {
                                            it.session.send(
                                                "${
                                                    Json.encodeToJsonElement(
                                                        NetworkPacket(
                                                            NetworkStatus.EXIT,
                                                            user
                                                        )
                                                    )
                                                }"
                                            )
                                            it.session.send("Removing ${thisConnection.name} user! ")
                                        } catch (e: Exception) {
                                            connections.removeConnection(thisConnection)
                                        }
                                    }
                                }
                            }
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        }
                        connectionsCopy = synchronized(connections) { connections.toSet() }
                        coroutineScope {
                            connectionsCopy.forEach {
                                launch(sendCancelledChannelErrorHandler) {

                                    try {
                                        it.session.send(receivedText)
                                    } catch (e: Exception) {
                                        connections.removeConnection(thisConnection)
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        throw BadRequestException("bad message format")
                    }
                }
            }
        } catch (e: R2dbcNonTransientResourceException) {
            close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, e.message ?: "null"))
        } catch (e: IOException) {
            close(CloseReason(CloseReason.Codes.NOT_CONSISTENT, e.message ?: "null"))
        } catch (e: Exception) {
            if (e.localizedMessage != "ArrayChannel was cancelled")
                println(e.message)
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, e.message ?: "null"))
        } finally {
//                println("Removing $thisConnection!")
            try {
                connections.removeConnection(thisConnection)
            } catch (e: Exception) {
                println(e)
            }
        }
    }
}