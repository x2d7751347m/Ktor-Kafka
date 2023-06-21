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
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRouting() {
    val service = Service()
    route("/api/v1/users", {
        request {
            queryParameter<String>("query parameter") {
                description = "query parameter description"
                example = "query parameter example"
            }
        }
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
        get {
            service.findUsers(1, 1)
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
            description = "create user."
            request {
                queryParameter<String>("query parameter") {
                    description = "query parameter description"
                    example = "query parameter example"
                }
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
            call.respond(status = HttpStatusCode.Created, service.createUser(user))
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
