package com.x2d7751347m.plugins

import com.x2d7751347m.Connection
import com.x2d7751347m.entity.NetworkPacket
import com.x2d7751347m.entity.NetworkStatus
import com.x2d7751347m.entity.User
import com.x2d7751347m.repository.UserRepository
import io.confluent.developer.extension.logger
import io.confluent.developer.ktor.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import io.r2dbc.spi.R2dbcNonTransientResourceException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import java.io.IOException
import java.io.InputStream
import java.time.Duration
import java.util.*
import kotlin.random.Random


fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    // load properties
//    val kafkaConfigPath = "src/main/resources/kafka.conf"
    val kafkaConfigPath = "app/resources/kafka.conf" // for docker image
    //region Kafka
    install(Kafka) {

        configurationPath = kafkaConfigPath
//        topics = listOf(
//            newTopic(ratingTopicName) {
//                partitions = 3
//                replicas = 1    // for docker
//                //replicas = 3  // for cloud
//            },
//            newTopic(ratingsAvgTopicName) {
//                partitions = 3
//                replicas = 1    // for docker
//                //replicas = 3  // for cloud
//            }
//        )
    }
    //endregion

}

fun Route.socketRouting() {
    val sendCancelledChannelErrorHandler =
        CoroutineExceptionHandler { context, exception ->
            println("CoroutineExceptionHandler got $exception")
        }
    val userRepository = UserRepository()
    val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet(2000))
    val connectionMutex = Mutex()

    suspend fun <Connection> MutableSet<Connection>.removeConnection(
        connection: Connection,
        session: WebSocketServerSession,
        closeReason: CloseReason?
    ) {
        try {
            connectionMutex.withLock {
                if (contains(connection)) {
                    remove(connection)
                }
            }
            closeReason?.run { session.close(this) } ?: run { session.close() }
        } catch (e: Exception) {
            throw InternalError(e)
        }
    }

    webSocket("v1/api/chat") {

        val thisConnection: Connection
        connectionMutex.withLock {
            thisConnection = Connection(this)
            connections.add(thisConnection)
        }
        val currentMoment: Instant = kotlinx.datetime.Clock.System.now()
        val principal = call.principal<JWTPrincipal>()
        var id: Long
        var user = User()
        user =
            User()
                .apply { this.id = Random(currentMoment.epochSeconds).nextLong() }
//            }
        println("Adding user!")
        try {
            send("You are connected! There are ${connections.count()} users here.")
            println("The User is connected! There are ${connections.count()} users here.")
            send(Json.encodeToJsonElement(NetworkPacket(NetworkStatus.ENTRY, user)).toString())
            coroutineScope {
                connectionMutex.withLock { connections.toSet() }.forEach {
                    launch(sendCancelledChannelErrorHandler) {
                        try {
                            it.session.send("${thisConnection.name} is connected! There are ${connections.count()} users here.")
                        } catch (e: Exception) {
                            connections.removeConnection(
                                thisConnection,
                                this@webSocket,
                                CloseReason(CloseReason.Codes.INTERNAL_ERROR, e.localizedMessage)
                            )
                        }
                    }
                }
            }
            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> {
                        val receivedByteArray = frame.readBytes()

                        coroutineScope {
                            connectionMutex.withLock { connections.toSet() }.forEach {
                                launch(sendCancelledChannelErrorHandler) {
                                    try {
                                        it.session.send(receivedByteArray)
                                    } catch (e: Exception) {
                                        connections.removeConnection(
                                            thisConnection,
                                            this@webSocket,
                                            CloseReason(CloseReason.Codes.INTERNAL_ERROR, e.localizedMessage)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is Frame.Text -> {
                        val receivedText = "${frame.readText()}"
                        val textWithUsername = "[${thisConnection.name}]: $receivedText"

//                            if (receivedText.startsWith("{") && Json.decodeFromJsonElement<NetworkPacket>(
//                                    Json.decodeFromString(
//                                        receivedText
//                                    )
//                                ).networkPacket == NetworkStatus.EXIT
                        if (receivedText.lowercase().startsWith("bye")
                        ) {
                            coroutineScope {
                                connectionMutex.withLock { connections.toSet() }.forEach {
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
                                            connections.removeConnection(
                                                thisConnection,
                                                this@webSocket,
                                                CloseReason(CloseReason.Codes.INTERNAL_ERROR, e.localizedMessage)
                                            )
                                        }
                                    }
                                }
                            }
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        } else if (receivedText.lowercase() == "connections") {
                            send("There are ${connections.count()} users here.")
                        }
                        coroutineScope {
                            connectionMutex.withLock { connections.toSet() }.forEach {
                                launch(sendCancelledChannelErrorHandler) {

                                    try {
                                        it.session.send(receivedText)
                                    } catch (e: Exception) {
                                        connections.removeConnection(
                                            thisConnection,
                                            this@webSocket,
                                            CloseReason(CloseReason.Codes.INTERNAL_ERROR, e.localizedMessage)
                                        )
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
            connections.removeConnection(
                thisConnection,
                this@webSocket,
                CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, e.localizedMessage)
            )
        } catch (e: IOException) {
            connections.removeConnection(
                thisConnection,
                this@webSocket,
                CloseReason(CloseReason.Codes.NOT_CONSISTENT, e.localizedMessage)
            )
        } catch (e: Exception) {
            connections.removeConnection(
                thisConnection,
                this@webSocket,
                CloseReason(CloseReason.Codes.INTERNAL_ERROR, e.localizedMessage)
            )
        } finally {
            println("Removing ${thisConnection.name} user by error! There are ${connections.count()} users here.")
            try {
                connections.removeConnection(
                    thisConnection,
                    this@webSocket,
                    CloseReason(CloseReason.Codes.INTERNAL_ERROR, "null")
                )
            } catch (_: Exception) {
            }
        }
    }

    authenticate("auth-jwt") {
        webSocket("/v1/api/messages/{group}") {

            val currentMoment: Instant = kotlinx.datetime.Clock.System.now()
            val log = logger<Application>()
            val group = call.parameters.getOrFail("group")

            val principal = call.principal<JWTPrincipal>()
            var id: Long
            var user = User()
            principal?.run {
                id = principal.payload.getClaim("id").asLong()
                user = userRepository.fetchUser(id)!!
            } ?: run {
                user =
                    User()
                        .apply {
                            this.id = Random(currentMoment.epochSeconds).nextLong()
                            this.nickname = this.id.toString()
                        }
            }
            println("Adding user!")
            send(Json.encodeToJsonElement(NetworkPacket(NetworkStatus.ENTRY, user)).toString())

            val config = ApplicationConfig("kafka.conf")
            val binaryProducer: KafkaProducer<String, ByteArray> = buildProducer(config)
            val textProducer: KafkaProducer<String, String> = buildTextProducer(config)


            val clientId = user.id
//        val clientId = call.parameters["clientId"] ?: "¯\\_(ツ)_/¯"
            val binaryConsumer: KafkaConsumer<String, ByteArray> =
                createKafkaConsumer(config, group, "ws-consumer-$clientId")
            val textConsumer: KafkaConsumer<String, String> =
                createKafkaTextConsumer(config, group, "ws-consumer-$clientId")
            try {
                coroutineScope {
                    launch(Dispatchers.IO) {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val receivedText = "${frame.readText()}"
                                    if (receivedText.lowercase().startsWith("bye")) {
                                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                                    }
                                    textProducer.send(group, user.nickname, receivedText)
                                }

                                is Frame.Binary -> {
                                    val receivedByteArray = frame.readBytes()

                                    binaryProducer.send(group, user.nickname, receivedByteArray)
                                }

                                else -> {
                                    throw BadRequestException("Invalid websocket frame")
                                }
                            }
                        }
                    }
//                    launch {
//                        while (true) {
//                            poll(binaryConsumer)
//                        }
//                    }
                    while (true) {
                        textPoll(textConsumer)
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                binaryProducer.abortTransaction()
                textProducer.abortTransaction()
                binaryConsumer.apply {
                    unsubscribe()
                }
                textConsumer.apply {
                    unsubscribe()
                }
                close()
                log.info("consumer for ${binaryConsumer.groupMetadata().groupId()} unsubscribed and closed...")
            }


        }
    }
}

suspend fun DefaultWebSocketServerSession.poll(consumer: KafkaConsumer<Long, ByteArray>) =
    withContext(Dispatchers.IO) {
        consumer.poll(Duration.ofMillis(100))
            .forEach {
                outgoing.send(
                    Frame.Binary(true, it.value())
                )
            }
    }

suspend fun DefaultWebSocketServerSession.textPoll(consumer: KafkaConsumer<String, String>) =
    withContext(Dispatchers.IO) {
        consumer.poll(Duration.ofMillis(100))
            .forEach {
                outgoing.send(
                    Frame.Text("[${it.key()}]: ${it.value()}")
                )
            }
    }

//raw socket

/**
 * Two mains are provided, you must first start EchoApp.Server, and then EchoApp.Client.
 * You can also start EchoApp.Server and then use a telnet client to connect to the echo server.
 */
object EchoApp {
    val selectorManager = ActorSelectorManager(Dispatchers.IO)
    val DefaultPort = 9002

    object Server {
        @JvmStatic
        fun server_main(args: Array<String>) {
            runBlocking {
                val serverSocket = aSocket(selectorManager).tcp().bind(port = DefaultPort)
                println("Echo Server listening at ${serverSocket.localAddress}")
                while (true) {
                    val socket = serverSocket.accept()
                    println("Accepted $socket")
                    launch {
                        val read = socket.openReadChannel()
                        val write = socket.openWriteChannel(autoFlush = true)
                        try {
                            while (true) {
                                val line = read.readUTF8Line()
                                write.writeStringUtf8("$line\n")
                            }
                        } catch (e: Throwable) {
                            socket.close()
                        }
                    }
                }
            }
        }
    }

    object Client {
        @JvmStatic
        fun client_main(args: Array<String>) {
            runBlocking {
                val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", port = DefaultPort)
                val read = socket.openReadChannel()
                val write = socket.openWriteChannel(autoFlush = true)

                launch(Dispatchers.IO) {
                    while (true) {
                        val line = read.readUTF8Line()
                        println("server: $line")
                    }
                }

                for (line in System.`in`.lines()) {
                    println("client: $line")
                    write.writeStringUtf8("$line\n")
                }
            }
        }

        private fun InputStream.lines() = Scanner(this).lines()

        private fun Scanner.lines() = sequence {
            while (hasNext()) {
                yield(readLine())
            }
        }
    }
}