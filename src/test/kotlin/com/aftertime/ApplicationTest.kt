package com.x2d7751347m

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
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket


@Testcontainers
class ApplicationTest {

    @Test
    fun test() {
    }

    @Test
    fun testRoot() = testApplication {
        application {
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
                endpoint.maxConnectionsPerRoute = 2000
                endpoint.socketTimeout = 15000
                endpoint.connectAttempts = 3
                endpoint.connectTimeout = 15000
            }
            install(WebSockets) {
                pingInterval = 15_000
            }
        }

        fun currentMoment(): Instant = Clock.System.now()
        coroutineScope {
            repeat(1000) {
                launch(handler) {
//                    var lastSentTime = mutableMapOf(Pair(it, currentMoment().toEpochMilliseconds()))
//                    val myMessage = "$it"
                    client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/chat") {
                        timeoutMillis = 15000
                        while (true) {
                            try {
//                                    if (currentMoment().minus(Instant.fromEpochMilliseconds(lastSentTime[it]!!)).inWholeMilliseconds > 1000) {
//                                        send(myMessage)
//                                        lastSentTime[it] = currentMoment().toEpochMilliseconds()
//                                    }
                                    when (incoming.receive()) {
                                        is Frame.Text -> {
                                            val othersMessage = incoming.receive() as? Frame.Text
                                            println("$it+${othersMessage?.readText()}")
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

    // for test raw socket
    @Test
    fun socketClient() {
        val client = Socket("127.0.0.1", 9002)
        val output = PrintWriter(client.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(client.inputStream))

        println("Client sending [Hello]")
        output.println("Hello")
        println("Client receiving [${input.readLine()}]")
        client.close()
    }
}
