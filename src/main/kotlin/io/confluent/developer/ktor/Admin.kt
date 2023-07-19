package io.confluent.developer.ktor

import TopicBuilder
import io.confluent.developer.extension.toMap
import io.ktor.server.config.*
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.CreateTopicsResult
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.TopicConfig
import java.util.*

/*
    example of Kafka Admin API usage
    TODO: write integration/TC test instead of inline example
 */
@SuppressWarnings("unused")
fun configureKafkaTopics(config: Properties, vararg topics: String): CreateTopicsResult {
    return kafkaAdmin(config) {
        createTopics(topics.map {
            newTopic(it) {
                partitions = 3
                replicas = 1
                configs = mapOf(
                    TopicConfig.CLEANUP_POLICY_COMPACT to "compact",
                )
            }
        })
    }
}

fun kafkaAdmin(props: Properties, block: AdminClient.() -> CreateTopicsResult): CreateTopicsResult =
    AdminClient.create(props).use(block)

fun newTopic(name: String, block: TopicBuilder.() -> Unit): NewTopic =
    TopicBuilder(name).apply(block).build()

fun kafkaAdmin(config: ApplicationConfig, block: AdminClient.() -> CreateTopicsResult): CreateTopicsResult =
    buildKafkaAdmin(config).use(block)

fun buildKafkaAdmin(config: ApplicationConfig): AdminClient {
    val bootstrapServers: List<String> = config.property("ktor.kafka.bootstrap.servers").getList()

    // common config
    val commonConfig = config.toMap("ktor.kafka.properties")
    val adminProperties: Properties = Properties().apply {
        putAll(commonConfig)
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    }

    return AdminClient.create(adminProperties)
}

