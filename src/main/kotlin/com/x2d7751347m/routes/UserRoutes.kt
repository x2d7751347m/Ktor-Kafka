package com.x2d7751347m.routes

import com.x2d7751347m.dto.UserPatch
import com.x2d7751347m.dto.UserPost
import com.x2d7751347m.dto.UserResponse
import com.x2d7751347m.entity.User
import com.x2d7751347m.entity.userStorage
import com.x2d7751347m.entity.validateUser
import com.x2d7751347m.mapper.UserMapper
import com.x2d7751347m.plugins.ConflictException
import com.x2d7751347m.plugins.ExceptionResponse
import com.x2d7751347m.plugins.ValidationExceptions
import com.x2d7751347m.repository.UserRepository
import io.github.smiley4.ktorswaggerui.dsl.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.toList
import org.mapstruct.factory.Mappers

fun Route.userRouting() {
    val userRepository = UserRepository()
    val userMapper = Mappers.getMapper(UserMapper::class.java)
    route("/api/v1/users", {
        tags = listOf("user")
    }) {
        post({
            description = "create user."
            request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }
                body<UserPost> {
                    example("First", UserPost(username = "username", nickname = "nickname", password = "Password12!")) {
                        description = "first example"
                    }
                    example(
                        "Second",
                        UserPost(username = "username2", nickname = "nickname2", password = "Password1234!")
                    ) {
                        description = "second example"
                    }
                    required = true
                }
            }
            response {
                HttpStatusCode.Created to {
                    description = "Created"
                }
            }
        }) {
            val user = userMapper.userPostToUser(call.receive<UserPost>())

            validateUser(user).errors.let {
                if (it.isNotEmpty()) throw ValidationExceptions(it)
            }
            userRepository.createUser(user)
//            CoroutineScope(Job()).launch { Mail().sendEmail("hahaha") }
            call.response.status(HttpStatusCode.Created)
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
                response {
                    HttpStatusCode.OK to {
                        description = "Successful Request"
                        body<List<UserResponse>> {
                            mediaType(ContentType.Application.Json)
                            description = "the response"
                        }
                    }
                }
            }) {
                val page = call.parameters["page"]?.toInt() ?: throw BadRequestException("page is null")
                val size = call.parameters["size"]?.toInt() ?: throw BadRequestException("size is null")
                call.respond(
                    userMapper.userListToUserResponseList(
                        userRepository.findUsers(page, size).toList()
                    )
                )
            }
            get("{id}", {
                request {
                    pathParameter<Long>("id") {
                        description = "id"
                        required = true
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        body<UserResponse> {
                            mediaType(ContentType.Application.Json)
                            description = "the response"
                        }
                    }
                }
            }) {
                val id = call.parameters["id"]?.toLong() ?: throw BadRequestException("id is null")
                val user =
                    userRepository.findUser(id) ?: throw NotFoundException()
                call.respond(user)
            }
            patch({
                request {
                    body<UserPatch> {
                        example("First", UserPatch(nickname = "nickname", password = "Password12!")) {
                            description = "nickname"
                        }
                        example("Second", UserPatch(nickname = "nickname2", password = "Password1234!")) {
                            description = "nickname2"
                        }
                        required = true
                    }
                }
            }) {
                val principal = call.principal<JWTPrincipal>()
                val id = principal!!.payload.getClaim("id").asLong()
                call.respond(
                    userRepository.patchUser(
                        userMapper.userPatchToUserData(
                            call.receive<UserPatch>()
                        ).apply { this.id = id }
                    )
                )
            }
            delete({
            }) {
                val principal = call.principal<JWTPrincipal>()
                val id = principal!!.payload.getClaim("id").asLong()
                userRepository.deleteUser(id)
                call.response.status(HttpStatusCode.OK)
            }
        }
    }
    route("/api/v1/admins", {
        description = "administrator role is required"
        tags = listOf("admin")
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
                call.respond(userRepository.findUsers(page, size))
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
                    userRepository.findUser(id) ?: throw NotFoundException()
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
                        example("First", User(username = "username", nickname = "nickname", password = "Password12!")) {
                            description = "nickname"
                        }
                        example(
                            "Second",
                            User(username = "username", nickname = "nickname2", password = "Password1234!")
                        ) {
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
                call.respond(status = HttpStatusCode.Created, userRepository.createAdmin(user))
            }
            patch("{id}", {
                request {
                    pathParameter<Long>("id") {
                        description = "id"
                        required = true
                    }
                    body<UserPatch> {
                        example("First", UserPatch(nickname = "nickname", password = "Password12!")) {
                            description = "nickname"
                        }
                        example("Second", UserPatch(nickname = "nickname2", password = "Password1234!")) {
                            description = "nickname2"
                        }
                        required = true
                    }
                }
            }) {
                call.respond(
                    userRepository.patchUser(
                        userMapper.userPatchToUserData(
                            call.receive<UserPatch>()
                        ).apply { id = call.parameters["id"]!!.toLong() }
                    )
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
}