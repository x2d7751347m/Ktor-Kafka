package com.aftertime.plugins

import com.aftertime.Service.Service
import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.dsl.get
import io.github.smiley4.ktorswaggerui.dsl.post
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Application.configureRouting() {
    val port = environment.config.propertyOrNull("ktor.deployment.db.port")!!.getString().toInt()
    val url1 = environment.config.propertyOrNull("ktor.deployment.db.url")!!.getString()
    val name = environment.config.propertyOrNull("ktor.deployment.db.name")!!.getString()
    val username = environment.config.propertyOrNull("ktor.deployment.db.username")!!.getString()
    val password = environment.config.propertyOrNull("ktor.deployment.db.password")!!.getString()
    install(AutoHeadResponse)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    install(SwaggerUI) {
        swagger {
            swaggerUrl = "swagger-ui"
            forwardRoot = true
        }
        info {
            title = "Pararium API"
            version = "latest"
            description = "Pararium API for testing and demonstration purposes."
        }
        server {
            url = "http://localhost:8080"
            description = "Development Server"
        }
    }
    install(Resources)
    routing {
        post("/api/v1/users/create", {
            description = "create user."
            response {
                HttpStatusCode.Created to {
                    description = "Successful Request"
                    body<String> { description = "the response" }
                }
                HttpStatusCode.BadRequest to {
                    description = "Not a valid request"
                }
                HttpStatusCode.InternalServerError to {
                    description = "Something unexpected happened"
                }
            }
        }) {
            call.respondText(
                "created." + Service().a(url1, port, name, username, password),
                status = HttpStatusCode.Created
            )
        }
        get("/hello", {
            description = "Hello World Endpoint."
            response {
                HttpStatusCode.OK to {
                    description = "Successful Request"
                    body<String> { description = "the response" }
                }
                HttpStatusCode.InternalServerError to {
                    description = "Something unexpected happened"
                }
            }
        }) {
            call.respondText("Hello World!" + Service().a(url1, port, name, username, password))
        }
        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
        get<Articles> { article ->
            // Get all articles ...
            call.respond("List of articles sorted starting from ${article.sort}")
        }
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    }
}

@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new")
