package com.aftertime.routes

import com.aftertime.entity.User
import com.aftertime.entity.UserData
import com.aftertime.entity.userStorage
import com.aftertime.entity.validateUser
import com.aftertime.plugins.ExceptionResponse
import com.aftertime.plugins.ValidationExceptions
import com.aftertime.repository.Repository
import io.github.smiley4.ktorswaggerui.dsl.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRouting() {
    val repository = Repository()
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
                    required = true
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
            call.respond(status = HttpStatusCode.Created, repository.createUser(user))
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
                call.respond(repository.findUsers(page, size))
            }
            get("{id}", {
                request {
                    pathParameter<Long>("id") {
                        description = "id"
                        required = true
                    }
                }
            }) {
                val id = call.parameters["id"]?.toLong() ?: throw BadRequestException("id is null")
                val user =
                    repository.findUser(id) ?: throw NotFoundException()
                call.respond(user)
            }
            patch("{id}", {
                request {
                    pathParameter<Long>("id") {
                        description = "id"
                        required = true
                    }
                    body<UserData> {
                        example("First", UserData(nickname = "nickname", password = "Password12!")) {
                            description = "nickname"
                        }
                        example("Second", UserData(nickname = "nickname2", password = "Password1234!")) {
                            description = "nickname2"
                        }
                        required = true
                    }
                }
            }) {
                call.respond(
                    repository.patchUser(
                        call.receive<UserData>().apply { id = call.parameters["id"]!!.toLong() })
                )
            }
            delete("{id}", {
                request {
                    pathParameter<Long>("id") {
                        description = "id"
                        required = true
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
//                    required = true
                }
                queryParameter<Int>("size") {
                    example = 10
//                    required = true
                }
            }
        }) {
            val page = call.parameters["page"]?.toInt() ?: throw BadRequestException("page is null")
            val size = call.parameters["size"]?.toInt() ?: throw BadRequestException("size is null")
            call.respond(repository.findUsers(page, size))
        }
        get("{id}", {
            request {
                pathParameter<Long>("id") {
                    description = "id"
                    required = true
                }
            }
        }) {
            val id = call.parameters["id"]?.toLong() ?: throw BadRequestException("id is null")
            val user =
                repository.findUser(id) ?: throw NotFoundException()
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
                    required = true
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
            call.respond(status = HttpStatusCode.Created, repository.createAdmin(user))
        }
        delete("{id}", {
            request {
                pathParameter<Long>("id") {
                    description = "id"
                    required = true
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