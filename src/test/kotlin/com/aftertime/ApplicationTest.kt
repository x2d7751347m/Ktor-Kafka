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

        val client = HttpClient(CIO) {
            engine {
                endpoint.maxConnectionsPerRoute = 800
            }
            install(WebSockets) {
                pingInterval = 15_000
            }
        }
        coroutineScope {
            repeat(900) {
                launch {
//                    delay(it.toLong() * 200)
                    client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/chat") {
//                        timeoutMillis = 15000
//                        pingIntervalMillis = 14000
                        // this: DefaultClientWebSocketSession
                        while (true) {
                            try {
                                when(incoming.receive()){
                                    is Frame.Text -> {
                                        val othersMessage = incoming.receive() as? Frame.Text
                                        println(othersMessage?.readText())
////                    val myMessage = Scanner(System.`in`).next()
                                        delay(100)
                                        val myMessage = "S"
                                        if (othersMessage != null) {
                                            send(myMessage)
                                        }
                                    }
                                    else -> {}
                                }

                            } catch (e: Exception) {
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}
