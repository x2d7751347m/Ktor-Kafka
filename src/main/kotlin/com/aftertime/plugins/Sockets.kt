package com.aftertime.plugins

import com.aftertime.Connection
import com.aftertime.entity.NetworkPacket
import com.aftertime.entity.NetworkStatus
import com.aftertime.entity.User
import io.confluent.developer.extension.logger
import io.confluent.developer.ktor.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.server.application.*
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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import java.io.IOException
import java.io.InputStream
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random


fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    // load properties
    val kafkaConfigPath = "src/main/resources/kafka.conf"
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
    val sendCancelledChannelErrorHandler = CoroutineExceptionHandler { _, exception ->
        println("CoroutineExceptionHandler got $exception")
    }
//    val service = Service()
    val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet(200))
    val connectionMutex = Mutex()

    suspend fun <Connection> MutableSet<Connection>.removeConnection(connection: Connection) {
        connectionMutex.withLock {
            if (contains(connection)) {
                remove(connection)
            }
        }
    }

//    authenticate("auth-jwt") {
    webSocket("/chat") {

        val thisConnection: Connection
        connectionMutex.withLock {
            thisConnection = Connection(this)
            connections.add(thisConnection)
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
                connectionMutex.withLock { connections.toSet() }.forEach {
                    launch(sendCancelledChannelErrorHandler) {
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

                        coroutineScope {
                            connectionMutex.withLock { connections.toSet() }.forEach {
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
                                            connections.removeConnection(thisConnection)
                                        }
                                    }
                                }
                            }
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        }
                        coroutineScope {
                            connectionMutex.withLock { connections.toSet() }.forEach {
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


    val lastId = AtomicLong(0)
    webSocket("/messages/{group}") {

        val currentMoment: Instant = kotlinx.datetime.Clock.System.now()
        val log = logger<Application>()
        val group = call.parameters.getOrFail("group")

//            val principal = call.principal<JWTPrincipal>()
//            val id = principal!!.payload.getClaim("id").asLong()
//            val user = service.findUser(id)!!
//            println("Adding user!")
        val user =
//                        service.findUser(1)!!
            User()
                .apply { id = Random(currentMoment.epochSeconds).nextLong() }
        send("You are connected! There are ${connections.count()} users here.")
        println("The User is connected! There are ${connections.count()} users here.")
        send(Json.encodeToJsonElement(NetworkPacket(NetworkStatus.ENTRY, user)).toString())

        val config = ApplicationConfig("kafka.conf")
        val binaryProducer: KafkaProducer<Long, ByteArray> = buildProducer(config)
        val textProducer: KafkaProducer<Long, String> = buildTextProducer(config)


        val clientId = user.id
//        val clientId = call.parameters["clientId"] ?: "¯\\_(ツ)_/¯"
        val binaryConsumer: KafkaConsumer<Long, ByteArray> =
            createKafkaConsumer(config, group, "ws-consumer-$clientId")
        val textConsumer: KafkaConsumer<Long, String> =
            createKafkaTextConsumer(config, group, "ws-consumer-$clientId")
        try {
            coroutineScope {
                launch {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val receivedText = "${frame.readText()}"

                                textProducer.send(group, user.id, receivedText)
                            }

                            is Frame.Binary -> {
                                val receivedByteArray = frame.readBytes()

                                binaryProducer.send(group, user.id, receivedByteArray)
                            }

                            else -> {
                                throw BadRequestException("Invalid websocket frame")
                            }
                        }
                    }
                }
                launch {
                    while (true) {
                        poll(binaryConsumer)
                    }
                }
//                launch {
//                    while (true) {
//                        textPoll(textConsumer)
//                    }
//                }
            }
        } finally {
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

suspend fun DefaultWebSocketServerSession.poll(consumer: KafkaConsumer<Long, ByteArray>) =
    withContext(Dispatchers.IO) {
        consumer.poll(Duration.ofMillis(100))
            .forEach {
                outgoing.send(
                    Frame.Binary(true, it.value())
                )
            }
    }

suspend fun DefaultWebSocketServerSession.textPoll(consumer: KafkaConsumer<Long, String>) =
    withContext(Dispatchers.IO) {
        consumer.poll(Duration.ofMillis(100))
            .forEach {
                outgoing.send(
                    Frame.Text(it.value())
                )
            }
    }

/**
 * Two mains are provided, you must first start EchoApp.Server, and then EchoApp.Client.
 * You can also start EchoApp.Server and then use a telnet client to connect to the echo server.
 */
object EchoApp {
    val selectorManager = ActorSelectorManager(Dispatchers.IO)
    val DefaultPort = 9002

    object Server {
        @JvmStatic
        fun main(args: Array<String>) {
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
        fun main(args: Array<String>) {
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