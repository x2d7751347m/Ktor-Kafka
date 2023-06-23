package com.aftertime

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.test.Test

class ApplicationTest {
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
                endpoint.maxConnectionsPerRoute = 1100
                endpoint.socketTimeout = 15000
                endpoint.connectAttempts = 3
            }
            install(WebSockets) {
                pingInterval = 15_000
            }
        }
        coroutineScope {
            repeat(100) {
                launch(handler) {
//                    delay(it.toLong() * 1)
                    client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/chat") {
                        timeoutMillis = 15000
                        while (true) {
                            try {
                                delay(100)
                                when (incoming.receive()) {
//                                    is Frame.Ping -> {
//                                        send(Frame.Pong(ByteArray(0)))
//                                    }
                                    is Frame.Text -> {
                                        val othersMessage = incoming.receive() as? Frame.Text
//                                        println(othersMessage?.readText())
////                    val myMessage = Scanner(System.`in`).next()
                                        val myMessage = "$it"
                                        if (othersMessage != null) {
                                            send(myMessage)
                                        }
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
