package com.aftertime.routes

import com.aftertime.Entity.User
import com.aftertime.Entity.userStorage
import com.aftertime.Entity.validateUser
import com.aftertime.Service.Service
import com.aftertime.plugins.ValidationException
import io.github.smiley4.ktorswaggerui.dsl.delete
import io.github.smiley4.ktorswaggerui.dsl.get
import io.github.smiley4.ktorswaggerui.dsl.post
import io.github.smiley4.ktorswaggerui.dsl.route
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRouting() {
    route("/api/v1/users", {
        response {
            HttpStatusCode.OK to {
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
        get {
            if (userStorage.isNotEmpty()) {
                call.respond(userStorage)
            } else {
                call.respondText("No users found", status = HttpStatusCode.OK)
            }
        }
        get("{id}", {
            request {
                pathParameter<String>("id") {
                    description = "id"
                }
            }
        }) {
            val id = call.parameters["id"]?.toLong() ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val customer =
                userStorage.find { it.id == id } ?: return@get call.respondText(
                    "No user with id $id",
                    status = HttpStatusCode.NotFound
                )
            call.respond(customer)
        }
        post({
            description = "create user."
            request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }
                body<User> {
                    example("First", User(nickname = "aaa")) {
                        description = "aaa"
                    }
                    example("Second", User(20, "nickname")) {
                        description = "Either an addition of 20 and 7 or a subtraction of 7 from 20"
                    }
                }
            }
            response {
                HttpStatusCode.Created to {
                    description = "Successful Request"
                    body<String> { description = "the response" }
                }
            }
        }) {
            val user = call.receive<User>()

            validateUser(user).errors.let {
                if (it.isNotEmpty()) throw ValidationException(it.map {
                    "${it.dataPath}-${it.message}"
                }.reduce { acc, validationError ->
                    acc + "\n" + validationError
                })
            }
            val result = Service().createUser(user)
            call.respond(status = HttpStatusCode.Created, User(nickname = "nick"))
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
