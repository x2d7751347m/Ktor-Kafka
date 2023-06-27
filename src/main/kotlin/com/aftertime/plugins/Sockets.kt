package com.aftertime.plugins

import com.aftertime.Connection
import com.aftertime.entity.NetworkPacket
import com.aftertime.entity.NetworkStatus
import com.aftertime.entity.User
import com.google.api.client.googleapis.testing.TestUtils
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.r2dbc.spi.R2dbcNonTransientResourceException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.ValueMapper
import java.io.IOException
import java.time.Duration
import java.util.*
import java.util.regex.Pattern


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
//    val service = Service()
    val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet(200))
    val connectionMutex = Mutex()
    fun <Connection> MutableSet<Connection>.addConnection(connection: Connection) {
        synchronized(connections) {
            if (!contains(connection)) {
                add(connection)
            }
        }
    }

    fun <Connection> MutableSet<Connection>.removeConnection(connection: Connection) {
        synchronized(connections) {
            if (contains(connection)) {
                remove(connection)
            }
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
        synchronized(connections) {
            thisConnection = Connection(this)
            connections.addConnection(thisConnection)
        }
//            val principal = call.principal<JWTPrincipal>()
//            val id = principal!!.payload.getClaim("id").asLong()
//            val user = service.findUser(id)!!
//            println("Adding user!")
        try {
            val user =
//                        service.findUser(1)!!
                User()
                    .apply { id = thisConnection.name }
            send("You are connected! There are ${connections.count()} users here.")
            println("The User is connected! There are ${connections.count()} users here.")
            send(Json.encodeToJsonElement(NetworkPacket(NetworkStatus.ENTRY, user)).toString())
            coroutineScope {
                synchronized(connections) { connections.toSet() }.forEach {
                    launch(sendCancelledChannelErrorHandler) {
                        try {
                            it.session.send("${thisConnection.name} is connected! There are ${connections.count()} users here.")
                        } catch (e: Exception) {
//                            connections.removeConnection(thisConnection)
                        }
                    }
                }
            }
            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> {
                        val receivedByteArray = frame.readBytes()

                        coroutineScope {
                            synchronized(connections) { connections.toSet() }.forEach {
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
                            coroutineScope {
                                synchronized(connections) { connections.toSet() }.forEach {
                                    launch(sendCancelledChannelErrorHandler) {
                                        try {
                                            it.session.send(
                                                Json.encodeToJsonElement(
                                                    NetworkPacket(
                                                        NetworkStatus.EXIT,
                                                        user
                                                    )
                                                ).toString()
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
                        coroutineScope {
                            synchronized(connections) { connections.toSet() }.forEach {
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
            println("Removing ${thisConnection.name} user by error! There are ${connections.count()} users here.")
            try {
                connections.removeConnection(thisConnection)
            } catch (e: Exception) {
                println(e)
            }
        }
    }
    webSocket("/messages/{group}") {

        val inputTopic = "streams-plaintext-input"
        val outputTopic = "streams-wordcount-output"

        val bootstrapServers: String = "localhost:9092"

        val streamsConfiguration: Properties = getStreamsConfiguration(bootstrapServers);

        val builder = StreamsBuilder()
        createWordCountStream(builder)
        val streams = KafkaStreams(builder.build(), streamsConfiguration)
        // Always (and unconditionally) clean local state prior to starting the processing topology.
        // We opt for this unconditional call here because this will make it easier for you to play around with the example
        // when resetting the application for doing a re-run (via the Application Reset Tool,
        // https://docs.confluent.io/platform/current/streams/developer-guide/app-reset-tool.html).
        //
        // The drawback of cleaning up local state prior is that your app must rebuilt its local state from scratch, which
        // will take time and will require reading all the state-relevant data from the Kafka cluster over the network.
        // Thus in a production scenario you typically do not want to clean up always as we do here but rather only when it
        // is truly needed, i.e., only under certain conditions (e.g., the presence of a command line flag for your app).
        // See `ApplicationResetExample.java` for a production-like example.
        streams.cleanUp();

        // Now run the processing topology via `start()` to begin processing its input data.
        streams.start();


        val group = call.parameters.getOrFail("group")

        val thisConnection: Connection
        synchronized(connections) {
            thisConnection = Connection(this)
            connections.addConnection(thisConnection)
        }
//            val principal = call.principal<JWTPrincipal>()
//            val id = principal!!.payload.getClaim("id").asLong()
//            val user = service.findUser(id)!!
//            println("Adding user!")
        try {
            val user =
//                        service.findUser(1)!!
                User()
                    .apply { id = thisConnection.name }
            send("You are connected! There are ${connections.count()} users here.")
            println("The User is connected! There are ${connections.count()} users here.")
            send(Json.encodeToJsonElement(NetworkPacket(NetworkStatus.ENTRY, user)).toString())
            coroutineScope {
                synchronized(connections) { connections.toSet() }.forEach {
                    launch(sendCancelledChannelErrorHandler) {
                        try {
                            it.session.send("${thisConnection.name} is connected! There are ${connections.count()} users here.")
                        } catch (e: Exception) {
//                            connections.removeConnection(thisConnection)
                        }
                    }
                }
            }
            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> {
                        val receivedByteArray = frame.readBytes()

                        coroutineScope {
                            synchronized(connections) { connections.toSet() }.forEach {
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
                            coroutineScope {
                                synchronized(connections) { connections.toSet() }.forEach {
                                    launch(sendCancelledChannelErrorHandler) {
                                        try {
                                            it.session.send(
                                                Json.encodeToJsonElement(
                                                    NetworkPacket(
                                                        NetworkStatus.EXIT,
                                                        user
                                                    )
                                                ).toString()
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
                        coroutineScope {
                            synchronized(connections) { connections.toSet() }.forEach {
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
            println("Removing ${thisConnection.name} user by error! There are ${connections.count()} users here.")
            try {
                connections.removeConnection(thisConnection)
            } catch (e: Exception) {
                println(e)
            }
        }
    }
}

fun getStreamsConfiguration(bootstrapServers: String?): Properties? {
    val streamsConfiguration = Properties()
    // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
    // against which the application is run.
    streamsConfiguration[StreamsConfig.APPLICATION_ID_CONFIG] = "wordcount-lambda-example"
    streamsConfiguration[StreamsConfig.CLIENT_ID_CONFIG] = "wordcount-lambda-example-client"
    // Where to find Kafka broker(s).
    streamsConfiguration[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
    // Specify default (de)serializers for record keys and for record values.
    streamsConfiguration[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
    streamsConfiguration[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
    // Records should be flushed every 10 seconds. This is less than the default
    // in order to keep this example interactive.
    streamsConfiguration[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = 10 * 1000
    // For illustrative purposes we disable record caches.
    streamsConfiguration[StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG] = 0
    // Use a temporary directory for storing state, which will be automatically removed after the test.
    streamsConfiguration[StreamsConfig.STATE_DIR_CONFIG] =
        TestUtils.tempDirectory().getAbsolutePath()
    return streamsConfiguration
}

fun createWordCountStream(builder: StreamsBuilder) {
    // Construct a `KStream` from the input topic "streams-plaintext-input", where message values
    // represent lines of text (for the sake of this example, we ignore whatever may be stored
    // in the message keys).  The default key and value serdes will be used.
    val textLines: KStream<String, String> = builder.stream(inputTopic)
    val pattern: Pattern = Pattern.compile("\\W+", Pattern.UNICODE_CHARACTER_CLASS)
    val wordCounts: KTable<String?, Long> =
        textLines // Split each text line, by whitespace, into words.  The text lines are the record
            // values, i.e. we can ignore whatever data is in the record keys and thus invoke
            // `flatMapValues()` instead of the more generic `flatMap()`.
            .flatMapValues(ValueMapper<String, Iterable<*>> { value: String ->
                Arrays.asList(
                    pattern.split(value.lowercase(Locale.getDefault()))
                )
            }) // Group the split data by word so that we can subsequently count the occurrences per word.
            // This step re-keys (re-partitions) the input data, with the new record key being the words.
            // Note: No need to specify explicit serdes because the resulting key and value types
            // (String and String) match the application's default serdes.
            .groupBy { keyIgnored: String?, word: String? -> word } // Count the occurrences of each word (record key).
            .count()

    // Write the `KTable<String, Long>` to the output topic.
    wordCounts.toStream().to(outputTopic, Produced.with(Serdes.String(), Serdes.Long()))
}