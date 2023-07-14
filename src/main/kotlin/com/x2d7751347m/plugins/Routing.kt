package com.x2d7751347m.plugins

import com.x2d7751347m.routes.userRouting
import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.dsl.AuthScheme
import io.github.smiley4.ktorswaggerui.dsl.AuthType
import io.github.smiley4.ktorswaggerui.dsl.get
import io.github.smiley4.ktorswaggerui.dsl.route
import io.konform.validation.ValidationError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ExceptionResponse(
    val message: String?,
    val code: Int,
)

class ValidationExceptions(val errors: List<ValidationError>) : Throwable()

class BadRequestException(
    override val message: String,
) : Throwable()

class NotFoundException(
    override val message: String,
) : Throwable()

class ValidationException(
    override val message: String,
) : Throwable()

class ParsingException(override val message: String) : Throwable()

fun Application.configureRouting() {
    install(AutoHeadResponse)
    install(StatusPages) {
        exception<Throwable> { call, throwable ->
            when (throwable) {
                is ValidationException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ExceptionResponse(
                            throwable.message,
                            HttpStatusCode.BadRequest.value
                        )
                    )
                }

                is ValidationExceptions -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ExceptionResponse(
                            throwable.errors.toString(),
                            HttpStatusCode.BadRequest.value
                        )
                    )
                }

                is NotFoundException -> {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ExceptionResponse(
                            throwable.message,
                            HttpStatusCode.NotFound.value
                        )
                    )
                }

                is ParsingException -> {
                    call.respond(
                        HttpStatusCode.ExpectationFailed,
                        ExceptionResponse(
                            throwable.message,
                            HttpStatusCode.ExpectationFailed.value
                        )
                    )
                }
            }
        }
//        exception<Throwable> { call, cause ->
//            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
//        }
        status(
            // any number of status codes can be mentioned
            HttpStatusCode.InternalServerError,
            HttpStatusCode.BadGateway,
        ) { call, statusCode ->
            when (statusCode) {
                HttpStatusCode.InternalServerError -> {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ExceptionResponse(
                            "Oops! internal server error at our end",
                            HttpStatusCode.InternalServerError.value
                        )
                    )
                }

                HttpStatusCode.BadGateway -> {
                    call.respond(
                        HttpStatusCode.BadGateway,
                        ExceptionResponse(
                            "Oops! We got a bad gateway. Fixing it. Hold on!",
                            HttpStatusCode.BadGateway.value
                        )
                    )
                }
            }
        }
    }
    val port = environment.config.property("ktor.deployment.port").getString()
    install(SwaggerUI) {
        swagger {
            swaggerUrl = "swagger-ui"
            forwardRoot = true
        }
        info {
            title = "Ktor-sample API"
            version = "latest"
            description = "Ktor-sample API for testing and demonstration purposes."
        }
        server {
            url = "http://localhost:$port"
            description = "Development Server"
        }
        // default value for "401 Unauthorized"-responses.
        // the name of the security scheme (see below) to use for each route when nothing else is specified
        defaultSecuritySchemeName = "jwt"
        defaultUnauthorizedResponse {
            description = "Username or password is invalid."
        }
        // specify a security scheme
        securityScheme("jwt") {
            type = AuthType.HTTP
            scheme = AuthScheme.BEARER
            bearerFormat = "JWT"
        }
//        // specify another security scheme
//        securityScheme("MyOtherSecurityScheme") {
//            type = AuthType.HTTP
//            scheme = AuthScheme.BASIC
//        }
    }
    install(Resources)
    routing {
        route("", {
            response {
                HttpStatusCode.OK to {
                    description = "Successful Request"
                }
                HttpStatusCode.BadRequest to {
                    description = "Not a valid request"
                    body<ExceptionResponse> { description = "the response" }
                }
                HttpStatusCode.InternalServerError to {
                    description = "Something unexpected happened"
                    body<ExceptionResponse> { description = "the response" }
                }
            }
        }) {
            securityRouting()
            userRouting()
            socketRouting()
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
        }
    }
}
