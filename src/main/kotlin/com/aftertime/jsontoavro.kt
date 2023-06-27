package com.aftertime

import io.confluent.examples.streams.avro.WikiFeed
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.ValueMapper
import java.io.IOException
import java.util.*

object JsonToAvroExample {
    const val JSON_SOURCE_TOPIC = "json-source"
    const val AVRO_SINK_TOPIC = "avro-sink"

    @JvmStatic
    fun main(args: Array<String>) {
        val bootstrapServers = if (args.size > 0) args[0] else "localhost:9092"
        val schemaRegistryUrl = if (args.size > 1) args[1] else "http://localhost:8081"
        val streams = buildJsonToAvroStream(
            bootstrapServers,
            schemaRegistryUrl
        )
        streams.start()

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(Thread { streams.close() })
    }

    fun buildJsonToAvroStream(
        bootstrapServers: String?,
        schemaRegistryUrl: String?,
    ): KafkaStreams {
        val streamsConfiguration = Properties()
        streamsConfiguration[StreamsConfig.APPLICATION_ID_CONFIG] = "json-to-avro-stream-conversion"
        streamsConfiguration[StreamsConfig.CLIENT_ID_CONFIG] = "json-to-avro-stream-conversion-client"
        streamsConfiguration[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        // Where to find the Confluent schema registry instance(s)
        streamsConfiguration[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = schemaRegistryUrl
        streamsConfiguration[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = SpecificAvroSerde::class.java
        streamsConfiguration[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
        streamsConfiguration[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        streamsConfiguration[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = 100 * 1000
        val objectMapper = ObjectMapper()
        val builder = StreamsBuilder()

        // read the source stream
        val jsonToAvroStream = builder.stream(
            JSON_SOURCE_TOPIC,
            Consumed.with(Serdes.String(), Serdes.String())
        )
        jsonToAvroStream.mapValues<Any?>(ValueMapper<String, Any?> { v: String? ->
            var wikiFeed: WikiFeed? = null
            try {
                val jsonNode: JsonNode = objectMapper.readTree(v)
                wikiFeed = WikiFeed(
                    jsonNode.get("user").asText(),
                    jsonNode.get("is_new").asBoolean(),
                    jsonNode.get("content").asText()
                )
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
            wikiFeed
        }).filter { k: String?, v: Any? -> v != null }.to(AVRO_SINK_TOPIC)
        return KafkaStreams(builder.build(), streamsConfiguration)
    }
}