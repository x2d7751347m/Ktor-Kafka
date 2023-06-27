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
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.JoinWindows
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.*
import kotlin.test.Test


@Testcontainers
class ApplicationTest {
    data class TopicConfig(val inStream1: String, val inStream2: String, val outStream: String)

    fun createTopics(allProps: Map<String, Any>, topicConfig: TopicConfig) = AdminClient.create(allProps).use {
        it.createTopics(
            listOf(
                NewTopic(topicConfig.inStream1, 1, 1),
                NewTopic(topicConfig.inStream2, 1, 1),
                NewTopic(topicConfig.outStream, 1, 1)
            )
        )
    }

    val streamConfig = mapOf<String, Any>(
        StreamsConfig.APPLICATION_ID_CONFIG to "streams-example",
        StreamsConfig.CLIENT_ID_CONFIG to "streams-example-client",
        StreamsConfig.COMMIT_INTERVAL_MS_CONFIG to 10L,
        StreamsConfig.POLL_MS_CONFIG to 10L,
        StreamsConfig.REPARTITION_PURGE_INTERVAL_MS_CONFIG to 300L,
        StreamsConfig.REPARTITION_PURGE_INTERVAL_MS_CONFIG to 500L,
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:8080",
        StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.String().javaClass.name,
        StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to Serdes.ByteArray().javaClass.name
    )

    fun getTopology(topicConfig: TopicConfig): Topology = StreamsBuilder().apply {
        val (inStream1, inStream2, outStream) = topicConfig
        stream(inStream1, Consumed.with(Serdes.String(), Serdes.ByteArray())).join(
            stream(inStream2, Consumed.with(Serdes.String(), Serdes.ByteArray())),
            { name, num -> "${String(name)} ${String(num)}".encodeToByteArray() },
            JoinWindows.ofTimeDifferenceAndGrace(Duration.ofSeconds(3), Duration.ofSeconds(1))
        ).to(outStream)
    }.build()

    fun consumerProps(groupId: String): Properties {
        val props = Properties()
        props["bootstrap.servers"] = "localhost:8080"
        props["auto.offset.reset"] = "earliest"
        props["group.id"] = groupId
        props["key.deserializer"] = ByteArrayDeserializer::class.java
        props["value.deserializer"] = ByteArrayDeserializer::class.java
        props["security.protocol"] = "PLAINTEXT"
        // Other consumer properties can be added here
        return props
    }

    private fun retrieveResultsFromOutputStream(topicConfig: TopicConfig): MutableList<String> {
        val results: MutableList<String> = mutableListOf()
        KafkaConsumer<Int, ByteArray>(consumerProps("baeldung")).use {
            it.subscribe(listOf(topicConfig.outStream))
            while (results.size < 4) {
                val records = it.poll(Duration.ofMillis(100))
                for (record in records) {
                    results.add(record.value().toString(Charsets.UTF_8))
                }
            }
        }
        return results
    }

    fun result(topicConfig: TopicConfig) {
        KafkaStreams(getTopology(topicConfig), StreamsConfig(streamConfig)).use {
            it.cleanUp()
            it.start()

            val results: MutableList<String> = retrieveResultsFromOutputStream(topicConfig)
        }
    }

    @Test
    fun test() {
    }

    companion object {
        @JvmStatic
        @Container
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
    }


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
