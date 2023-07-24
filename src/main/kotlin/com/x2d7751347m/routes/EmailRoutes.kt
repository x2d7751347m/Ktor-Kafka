package com.x2d7751347m.routes

import com.x2d7751347m.dto.*
import com.x2d7751347m.entity.Email
import com.x2d7751347m.entity.validateEmail
import com.x2d7751347m.entity.validateEmailData
import com.x2d7751347m.mapper.EmailMapper
import com.x2d7751347m.plugins.ExceptionResponse
import com.x2d7751347m.plugins.ValidationExceptions
import com.x2d7751347m.repository.EmailRepository
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

fun Route.emailRouting() {
    val emailRepository = EmailRepository()
    val emailMapper = Mappers.getMapper(EmailMapper::class.java)
    route("/v1/api/user/emails", {
        tags = listOf("email")
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

            post({
                description = "create email."
                request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }
                    body<EmailUserPost> {
                        example("First", EmailUserPost(address = "address@domail.com")) {
                            description = "First example"
                        }
                        example(
                            "Second",
                            EmailUserPost(address = "address2@domail.com")
                        ) {
                            description = "Second example"
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
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("id").asLong()
                val email = emailMapper.emailUserPostToEmail(call.receive<EmailUserPost>()).apply { this.userId = userId }
                validateEmail(email).errors.let {
                    if (it.isNotEmpty()) throw ValidationExceptions(it)
                }
                emailRepository.insertEmail(email)
//            CoroutineScope(Job()).launch { Mail().sendEmail("hahaha") }
                call.response.status(HttpStatusCode.Created)
            }
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
                        body<List<EmailResponse>> {
                            mediaType(ContentType.Application.Json)
                            description = "the response"
                        }
                    }
                }
            }) {
                val page = call.parameters["page"]?.toInt() ?: throw BadRequestException("page is null")
                val size = call.parameters["size"]?.toInt() ?: throw BadRequestException("size is null")
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("id").asLong()
                call.respond(
                    emailMapper.emailListToEmailResponseList(
                        emailRepository.fetchEmailsByUserId(page, size, userId).toList()
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
                        body<EmailResponse> {
                            mediaType(ContentType.Application.Json)
                            description = "the response"
                        }
                    }
                }
            }) {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("id").asLong()
                val id = call.parameters["id"]?.toLong() ?: throw BadRequestException("id is null")
                val email =
                    emailRepository.fetchEmail(id) ?: throw NotFoundException()
                if(email.userId != userId) throw NotFoundException()
                call.respond(email)
            }
            patch("{id}", {
                request {
                    pathParameter<Long>("id") {
                        description = "id"
                        required = true
                    }
                    body<EmailUserPatch> {
                        example("First", EmailUserPatch(address = "address@domail.com", )) {
                            description = "First Example"
                        }
                        example("Second", EmailUserPatch(address = "address2@domail.com", )) {
                            description = "Second Example"
                        }
                        required = true
                    }
                }
            }) {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("id").asLong()
                val id = call.parameters["id"]?.toLong() ?: throw BadRequestException("id is null")
                if(emailRepository.fetchEmail(id)!!.userId!=userId){
                    throw NotFoundException()
                }
                call.respond(
                    emailRepository.updateEmail(
                        emailMapper.emailUserPatchToEmailData(
                            call.receive<EmailUserPatch>()
                        ).apply {
                            validateEmailData(this).errors.let {
                                if (it.isNotEmpty()) throw ValidationExceptions(it)
                            }
                            this.id = id }
                    )
                )
            }
            delete({
            }) {
                val principal = call.principal<JWTPrincipal>()
                val id = principal!!.payload.getClaim("id").asLong()
                emailRepository.deleteAllEmailsByUserId(id)
                call.response.status(HttpStatusCode.OK)
            }
        }
    }
    route("/v1/api/admin/emails", {
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
                call.respond(emailRepository.fetchEmails(page, size))
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
                val email =
                    emailRepository.fetchEmail(id) ?: throw NotFoundException()
                call.respond(email)
            }
            post({
                description = "create email."
                request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }
                    body<Email> {
                        example("First", Email(address = "address@domail.com", userId = 1)) {
                            description = "First Example"
                        }
                        example(
                            "Second",
                            Email(address = "address2@domail.com", userId = 2)
                        ) {
                            description = "Second Example"
                        }
                        required = true
                    }
                }
                response {
                    HttpStatusCode.Created to {
                        description = "Successful Request"
                        body<Email> {
                            mediaType(ContentType.Application.Json)
                            description = "the response"
                        }
                    }
                }
            }) {
                val email = call.receive<Email>()
                validateEmail(email).errors.let {
                    if (it.isNotEmpty()) throw ValidationExceptions(it)
                }
                call.respond(status = HttpStatusCode.Created, emailRepository.insertEmail(email))
            }
            patch("{id}", {
                request {
                    pathParameter<Long>("id") {
                        description = "id"
                        required = true
                    }
                    body<EmailPatch> {
                        example("First", EmailPatch(address = "address@domail.com", userId = 1)) {
                            description = "First Example"
                        }
                        example("Second", EmailPatch(address = "address2@domail.com", userId = 2)) {
                            description = "Second Example"
                        }
                        required = true
                    }
                }
            }) {
                call.respond(
                    emailRepository.updateEmail(
                        emailMapper.emailPatchToEmailData(
                            call.receive<EmailPatch>()
                        ).apply {
                            validateEmailData(this).errors.let {
                                if (it.isNotEmpty()) throw ValidationExceptions(it)
                            }
                            id = call.parameters["id"]!!.toLong() }
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
                if (emailRepository.fetchEmail(id)!=null) {
                    emailRepository.deleteEmail(id)
                    call.respondText("Customer removed correctly", status = HttpStatusCode.Accepted)
                } else {
                    call.respondText("Not Found", status = HttpStatusCode.NotFound)
                }
            }
        }
    }
}