package com.aftertime.plugins

import com.aftertime.routes.userRouting
import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.dsl.get
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(AutoHeadResponse)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "400: $cause", status = HttpStatusCode.BadRequest)
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
        userRouting()
        get("/health", {
            description = "health check Endpoint."
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
            call.respondText("Healthy!")
        }
//        // Static plugin. Try to access `/static/index.html`
//        static("/static") {
//            resources("static")
//        }
//        get<Articles> { article ->
//            // Get all articles ...
//            call.respond("List of articles sorted starting from ${article.sort}")
//        }
    }
}

//@Serializable
//@Resource("/articles")
//class Articles(val sort: String? = "new")
