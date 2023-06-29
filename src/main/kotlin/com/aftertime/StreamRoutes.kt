package com.aftertime

import com.aftertime.kstreams.Rating
import com.aftertime.kstreams.ratingTopicName
import com.aftertime.kstreams.ratingsAvgTopicName
import io.confluent.developer.extension.logger
import io.confluent.developer.ktor.buildProducer
import io.confluent.developer.ktor.createKafkaConsumer
import io.confluent.developer.ktor.send
import io.github.smiley4.ktorswaggerui.dsl.post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import java.time.Duration

fun Route.streamRouting(testing: Boolean = false) {
    val log = logger<Application>()

    //https://youtrack.jetbrains.com/issue/KTOR-2318
    val config = ApplicationConfig("kafka.conf")
    val producer: KafkaProducer<Long, Rating> = buildProducer(config)

    route("") {
        post("rating", {
            description = "post rating."
            request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }
                body<Rating> {
                    example("First", Rating(movieId = 1, rating = 1.1)) {
                        description = "rating 1"
                    }
                    example("Second", Rating(movieId = 2, rating = 2.2)) {
                        description = "rating 2"
                    }
                    required = true
                }
            }
        }) {
            val rating = call.receive<Rating>()

            producer.send(ratingTopicName, rating.movieId, rating)

            @Serializable
            data class Status(val message: String)
            call.respond(HttpStatusCode.Accepted, Status("Accepted"))
        }

        webSocket("/kafka") {

            val clientId = call.parameters["clientId"] ?: "¯\\_(ツ)_/¯"
            log.debug("clientId {}", clientId)
            val consumer: KafkaConsumer<Long, Double> =
                createKafkaConsumer(config, ratingsAvgTopicName, "ws-consumer-$clientId")
            try {
                while (true) {
                    poll(consumer)
                }
            } finally {
                consumer.apply {
                    unsubscribe()
                    //close()
                }
                log.info("consumer for ${consumer.groupMetadata().groupId()} unsubscribed and closed...")
            }
        }
    }
}

// https://discuss.kotlinlang.org/t/calling-blocking-code-in-coroutines/2368/5
// https://discuss.kotlinlang.org/t/coroutines-with-blocking-apis-such-as-jdbc/10669/4
// https://stackoverflow.com/questions/57650163/how-to-properly-make-blocking-service-calls-with-kotlin-coroutines
private suspend fun DefaultWebSocketServerSession.poll(consumer: KafkaConsumer<Long, Double>) =
    withContext(Dispatchers.IO) {
        consumer.poll(Duration.ofMillis(100))
            .forEach {
                outgoing.send(
                    Frame.Text(
                        """{
                                "movieId":${it.key()},
                                "rating":${it.value()}
                                }
                            """.trimIndent()
                    )
                )
            }
    }

