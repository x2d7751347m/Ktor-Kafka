package com.x2d7751347m.routes

import com.x2d7751347m.dto.UserPatch
import com.x2d7751347m.dto.UserPost
import com.x2d7751347m.dto.UserResponse
import com.x2d7751347m.entity.User
import com.x2d7751347m.entity.validateUser
import com.x2d7751347m.entity.validateUserPost
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
    route("/v1/api/user/users", {
        tags = listOf("user")
    }) {
        post({
            summary = "create user."
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
            val userPost = call.receive<UserPost>()
            validateUserPost(userPost)
            val user = userMapper.userPostToUser(userPost)
            validateUser(user).errors.let {
                if (it.isNotEmpty()) throw ValidationExceptions(it)
            }
            userRepository.insertUser(user)
//            CoroutineScope(Job()).launch { Mail().sendEmail("hahaha") }
            call.response.status(HttpStatusCode.Created)
        }
        post("available-nicknames", {
            summary = "check available nicknames."
            request {
                queryParameter<String>("nickname") {
                    description = "nickname"
                    example = "nickname1"
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "OK"
                }
            }
        }) {
            val nickname = call.parameters["nickname"] ?: throw BadRequestException("nickname is null")
            userRepository.fetchUserByNickName(nickname)?.run { throw ConflictException("nickname is already in use") }
//            CoroutineScope(Job()).launch { Mail().sendEmail("hahaha") }
            call.response.status(HttpStatusCode.OK)
        }
        post("available-emails", {
            summary = "check available emails."
            request {
                queryParameter<String>("email") {
                    description = "email"
                    example = "email@domain.com"
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "OK"
                }
            }
        }) {
            val email = call.parameters["email"] ?: throw BadRequestException("email is null")
            userRepository.fetchUserByEmail(email)?.run { throw ConflictException("email is already in use") }
//            CoroutineScope(Job()).launch { Mail().sendEmail("hahaha") }
            call.response.status(HttpStatusCode.OK)
        }
    }
    route("/v1/api/user/users", {
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
                summary = "get users"
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
                        userRepository.fetchUsers(page, size).toList()
                    )
                )
            }
            get("{id}", {
                summary = "get user by id"
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
                    userRepository.fetchUser(id) ?: throw NotFoundException()
                call.respond(user)
            }
            get("me", {
                summary = "get user information of mine"
                response {
                    HttpStatusCode.OK to {
                        body<UserResponse> {
                            mediaType(ContentType.Application.Json)
                            description = "the response"
                        }
                    }
                }
            }) {
                val principal = call.principal<JWTPrincipal>()
                val id = principal!!.payload.getClaim("id").asLong()
                val user =
                    userRepository.fetchUser(id) ?: throw NotFoundException()
                call.respond(user)
            }
            patch({
                summary = "patch user"
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
                    userRepository.updateUser(
                        userMapper.userPatchToUserData(
                            call.receive<UserPatch>()
                        ).apply { this.id = id }
                    )
                )
            }
            delete({
                summary = "delete user"
            }) {
                val principal = call.principal<JWTPrincipal>()
                val id = principal!!.payload.getClaim("id").asLong()
                userRepository.deleteUser(id)
                call.response.status(HttpStatusCode.OK)
            }
        }
    }
    route("/v1/api/admin/admins", {
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
                call.respond(userRepository.fetchUsers(page, size))
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
                    userRepository.fetchUser(id) ?: throw NotFoundException()
                call.respond(user)
            }
            post({
                description = "create account."
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
                call.respond(status = HttpStatusCode.Created, userRepository.insertAdmin(user))
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
                    userRepository.updateUser(
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
                if (userRepository.fetchUser(id)==null) {
                    userRepository.deleteUser(id)
                    call.respondText("Customer removed correctly", status = HttpStatusCode.Accepted)
                } else {
                    call.respondText("Not Found", status = HttpStatusCode.NotFound)
                }
            }
        }
    }
}