package com.aftertime.routes

import com.aftertime.Entity.User
import com.aftertime.Entity.userStorage
import com.aftertime.Entity.validateUser
import com.aftertime.Service.Service
import com.aftertime.plugins.ExceptionResponse
import com.aftertime.plugins.ValidationExceptions
import io.github.smiley4.ktorswaggerui.dsl.delete
import io.github.smiley4.ktorswaggerui.dsl.get
import io.github.smiley4.ktorswaggerui.dsl.post
import io.github.smiley4.ktorswaggerui.dsl.route
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRouting() {
    val service = Service()
    route("/api/v1/users", {
        tags = listOf("user")
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
        post({
            description = "create user."
            request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }
                body<User> {
                    example("First", User(nickname = "nickname", password = "Password12!")) {
                        description = "nickname"
                    }
                    example("Second", User(nickname = "nickname2", password = "Password1234!")) {
                        description = "nickname2"
                    }
                }
            }
            response {
                HttpStatusCode.Created to {
                    description = "Successful Request"
                    body<User> {
                        mediaType(ContentType.Application.Json)
                        description = "the response"
                    }
                }
            }
        }) {
            val user = call.receive<User>().apply { this.username = this.nickname }

            validateUser(user).errors.let {
                if (it.isNotEmpty()) throw ValidationExceptions(it)
            }
            call.respond(status = HttpStatusCode.Created, service.createUser(user))
        }
    }
    route("/api/v1/users", {
        tags = listOf("user")
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
        authenticate("auth-jwt") {
            get({
                request {
                    queryParameter<Int>("page") {
                        example = 1
                    }
                    queryParameter<Int>("size") {
                        example = 10
                    }
                }
            }) {
                val page = call.parameters["page"]?.toInt() ?: throw BadRequestException("page is null")
                val size = call.parameters["size"]?.toInt() ?: throw BadRequestException("size is null")
                call.respond(service.findUsers(page, size))
            }
            get("{id}", {
                request {
                    pathParameter<String>("id") {
                        description = "id"
                    }
                }
            }) {
                val id = call.parameters["id"]?.toLong() ?: throw BadRequestException("id is null")
                val user =
                    service.findUser(id) ?: throw NotFoundException()
                call.respond(user)
            }
            delete("{id}", {
                request {
                    pathParameter<String>("id") {
                        description = "id"
                    }
                }
            }) {
                val id = call.parameters["id"]?.toLong() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                if (userStorage.removeIf { it.id == id }) {
                    call.respondText("Customer removed correctly", status = HttpStatusCode.Accepted)
                } else {
                    call.respondText("Not Found", status = HttpStatusCode.NotFound)
                }
            }
        }
    }
    route("/api/v1/admins", {
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
        get({
            request {
                queryParameter<Int>("page") {
                    example = 1
                }
                queryParameter<Int>("size") {
                    example = 10
                }
            }
        }) {
            val page = call.parameters["page"]?.toInt() ?: throw BadRequestException("page is null")
            val size = call.parameters["size"]?.toInt() ?: throw BadRequestException("size is null")
            call.respond(service.findUsers(page, size))
        }
        get("{id}", {
            request {
                pathParameter<String>("id") {
                    description = "id"
                }
            }
        }) {
            val id = call.parameters["id"]?.toLong() ?: throw BadRequestException("id is null")
            val user =
                service.findUser(id) ?: throw NotFoundException()
            call.respond(user)
        }
        post({
            description = "create admin."
            request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }
                body<User> {
                    example("First", User(nickname = "nickname", password = "Password12!")) {
                        description = "nickname"
                    }
                    example("Second", User(nickname = "nickname2", password = "Password1234!")) {
                        description = "nickname2"
                    }
                }
            }
            response {
                HttpStatusCode.Created to {
                    description = "Successful Request"
                    body<User> {
                        mediaType(ContentType.Application.Json)
                        description = "the response"
                    }
                }
            }
        }) {
            val user = call.receive<User>()

            validateUser(user).errors.let {
                if (it.isNotEmpty()) throw ValidationExceptions(it)
            }
            call.respond(status = HttpStatusCode.Created, service.createAdmin(user))
        }
        delete("{id}", {
            request {
                pathParameter<String>("id") {
                    description = "id"
                }
            }
        }) {
            val id = call.parameters["id"]?.toLong() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (userStorage.removeIf { it.id == id }) {
                call.respondText("Customer removed correctly", status = HttpStatusCode.Accepted)
            } else {
                call.respondText("Not Found", status = HttpStatusCode.NotFound)
            }
        }
    }
}