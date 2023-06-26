package com.aftertime

import com.aftertime.entity.NetworkPacket
import com.aftertime.entity.NetworkStatus
import com.aftertime.entity.User
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test

class ApplicationTest {

    @Test
    fun test() =
        println(
            Json.encodeToJsonElement(
                NetworkPacket(
                    NetworkStatus.EXIT,
                    user = User(nickname = "haha")
                )
            ).toString()
        )

    @Test
    fun testRoot() = testApplication {
        application {
//            configureRouting()
        }
        client.get("/health").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Healthy!", bodyAsText())
        }
    }

    @Test
    fun testWebSocket() = testApplication {
        application {
//                testModule()
        }

        val handler = CoroutineExceptionHandler { _, exception ->
            println("CoroutineExceptionHandler got $exception")
        }
        val client = HttpClient(CIO) {
            engine {
                endpoint.maxConnectionsPerRoute = 200
                endpoint.socketTimeout = 15000
                endpoint.connectAttempts = 3
            }
            install(WebSockets) {
                pingInterval = 15_000
            }
        }

        fun currentMoment(): Instant = Clock.System.now()
        coroutineScope {
            repeat(100) {
                launch(handler) {
                    var lastSentTime = currentMoment()
                    val myMessage = "$it"
                    client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/chat") {
                        timeoutMillis = 15000
                        while (true) {
                            try {
                                if (currentMoment().minus(lastSentTime).inWholeMilliseconds > 500) {
                                    send(myMessage)
                                    lastSentTime = currentMoment()
                                }
                                when (incoming.receive()) {
                                    is Frame.Text -> {
                                        val othersMessage = incoming.receive() as? Frame.Text
//                                        println(othersMessage?.readText())
////                    val myMessage = Scanner(System.`in`).next()
                                    }

                                    else -> {}
                                }

                            } catch (e: Exception) {
                                println(e.localizedMessage)
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}
