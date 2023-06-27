package com.aftertime

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.JoinWindows
import java.time.Duration
import java.util.*


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

fun result(topicConfig: TopicConfig): MutableList<String> {
    KafkaStreams(getTopology(topicConfig), StreamsConfig(streamConfig)).use {
        it.cleanUp()
        it.start()

        return retrieveResultsFromOutputStream(topicConfig)
    }
}