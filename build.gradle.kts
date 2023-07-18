val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val prometeus_version: String by project

plugins {
//    application
    kotlin("jvm") version "1.8.22"
    id("io.ktor.plugin") version "2.3.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.22"
    id("com.google.devtools.ksp") version "1.8.22-1.0.11"
    id("com.avast.gradle.docker-compose") version "0.16.12"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("kapt") version "1.8.20"
}
kapt {
    correctErrorTypes = true
}
group = "com.x2d7751347m"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

ktor {
    fatJar {
        archiveFileName.set("ktor-kafka-$version.jar")
    }
}

repositories {
    mavenCentral()
    maven("https://repository.mulesoft.org/nexus/content/repositories/public/") {
        content {
            includeModule("com.github.everit-org.json-schema", "org.everit.json.schema")
        }
    }
    maven("https://packages.confluent.io/maven") {
        content {
            includeGroup("io.confluent")
            includeModule("org.apache.kafka", "kafka-clients")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val komapperVersion = "1.11.0"
val testcontainers_version: String by project
val ak_version: String by project
val confluent_version: String by project
dependencies {
    implementation(project(":plugin"))
    implementation("io.ktor:ktor-server-auto-head-response-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-apache-jvm:$ktor_version")
    implementation("jakarta.mail:jakarta.mail-api:2.1.2")
    implementation("org.eclipse.angus:jakarta.mail:2.0.2")
    implementation("io.ktor:ktor-server-partial-content-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-http-redirect-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-forwarded-header-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-conditional-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-caching-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-metrics-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktor_version")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometeus_version")
    implementation("io.ktor:ktor-server-hsts-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-compression-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-id-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-swagger-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-openapi:$ktor_version")
    implementation("io.ktor:ktor-server-resources:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    platform("org.komapper:komapper-platform:$komapperVersion").let {
        implementation(it)
        ksp(it)
    }
    implementation("org.komapper:komapper-starter-r2dbc")
    implementation("org.komapper:komapper-dialect-mariadb-r2dbc")
    ksp("org.komapper:komapper-processor")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("io.github.smiley4:ktor-swagger-ui:2.2.0")
    implementation("io.konform:konform:0.4.0")
    implementation("de.svenkubiak:jBCrypt:0.4.1")
    implementation("com.google.api-client:google-api-client:2.2.0")
    implementation("com.google.api-client:google-api-client-gson:2.2.0")
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    kapt("org.mapstruct:mapstruct-processor:1.5.5.Final")
    kaptTest("org.mapstruct:mapstruct-processor:1.5.5.Final")
    testImplementation("org.testcontainers:testcontainers:$testcontainers_version")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainers_version")
    testImplementation("org.testcontainers:kafka:$testcontainers_version")
    implementation("org.apache.kafka:kafka-clients:$ak_version")
    implementation("org.apache.kafka:kafka-streams:$ak_version")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$ak_version")
    implementation("io.confluent:kafka-json-schema-serializer:$confluent_version") {
        exclude("maven", "commons-collections")
    }
    implementation("io.confluent:kafka-streams-json-schema-serde:$confluent_version") {
        exclude("org.apache.kafka", "kafka-clients")
    }
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}