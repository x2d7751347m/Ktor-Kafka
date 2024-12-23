package com.x2d7751347m.routes

import com.x2d7751347m.dto.ImageFileResponse
import com.x2d7751347m.entity.ImageFile
import com.x2d7751347m.mapper.ImageFileMapper
import com.x2d7751347m.plugins.ExceptionResponse
import com.x2d7751347m.plugins.ImageUtil
import com.x2d7751347m.repository.ImageFileRepository
import io.github.smiley4.ktorswaggerui.dsl.delete
import io.github.smiley4.ktorswaggerui.dsl.get
import io.github.smiley4.ktorswaggerui.dsl.post
import io.github.smiley4.ktorswaggerui.dsl.route
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.r2dbc.spi.Blob
import kotlinx.coroutines.flow.toList
import reactor.core.publisher.Mono
import java.io.File
import java.nio.ByteBuffer

data class Metadata(
    val format: String,
    val location: Coords,
)

data class Coords(
    val lat: Float,
    val long: Float,
)

private fun byteArrayToBlob(byteArray: ByteArray): Blob =
    Blob.from(Mono.just(ByteBuffer.wrap(ImageUtil.compressImage(byteArray))))

fun Route.imageFileRouting() {
    val imageFileRepository = ImageFileRepository()
    route("/v1/api/user/image-files", {
        tags = listOf("image-file")
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
        get("{id}", {
            summary = "get imageFile by id"
            request {
                pathParameter<Long>("id") {
                    description = "id"
                    required = true
                }
            }
            response {
                HttpStatusCode.OK to {
                    header<String>(HttpHeaders.ContentType)
                    body<ContentType.Image> {
                        mediaType(ContentType.Image.PNG)
                        description = "the response"
                    }
                }
            }
        }) {
            val id = call.parameters["id"]?.toLong() ?: throw BadRequestException("id is null")
            val imageFile =
                imageFileRepository.fetchImageFile(id) ?: throw NotFoundException()
            call.response.headers.append(HttpHeaders.ContentType, "image/png")
            call.respondBytes(
                ImageFileMapper().imageFileToImageFileResponse(imageFile).data!!
            )
        }
        authenticate("auth-jwt") {
            get({
                summary = "get imageFiles"
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
                        body<List<ImageFileResponse>> {
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
                    ImageFileMapper().imageFileListToImageFileResponseList(
                        imageFileRepository.fetchImageFilesByUserId(
                            page,
                            size,
                            userId
                        ).toList()
                    ).onEach { it.data = ImageUtil.decompressImage(it.data!!) }
                )
            }
            post({
                summary = "create imageFile."
                request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }

                    multipartBody {
                        required = true
                        description = "profile image"
                        mediaType(ContentType.MultiPart.FormData)
                        part<File>("profileImage") {
                            mediaTypes = setOf(
                                ContentType.Image.PNG,
                                ContentType.Image.JPEG,
                                ContentType.Image.GIF
                            )
                        }
//                        part<Metadata>("myMetadata")
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

                var fileDescription = ""
                var fileName = ""
                val multipartData = call.receiveMultipart()


                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            fileDescription = part.value
                        }

                        is PartData.FileItem -> {
                            fileName = part.originalFileName as String
                            val fileBytes = part.streamProvider().readBytes()
                            imageFileRepository.insertImageFile(
                                ImageFile(
                                    name = fileName, data = byteArrayToBlob(fileBytes),
                                    userId = userId
                                )
                            )
                        }

                        else -> {}
                    }
                    part.dispose()
                }
//                imageFileRepository.insertImageFile(imageFile)
//            CoroutineScope(Job()).launch { Mail().sendImageFile("hahaha") }
                call.response.status(HttpStatusCode.Created)
            }
            delete({
                summary = "delete imageFiles"
            }) {
                val principal = call.principal<JWTPrincipal>()
                val id = principal!!.payload.getClaim("id").asLong()
                imageFileRepository.deleteAllImageFilesByUserId(id)
                call.response.status(HttpStatusCode.OK)
            }
        }
    }
    route("/v1/api/admin/imageFiles", {
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
                summary = "get imageFiles"
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
                        body<List<ImageFileResponse>> {
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
                    ImageFileMapper().imageFileListToImageFileResponseList(
                        imageFileRepository.fetchImageFilesByUserId(
                            page,
                            size,
                            userId
                        ).toList()
                    ).onEach { it.data = ImageUtil.decompressImage(it.data!!) }
                )
            }
            get("{id}", {
                summary = "get imageFile by id"
                request {
                    pathParameter<Long>("id") {
                        description = "id"
                        required = true
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        header<String>(HttpHeaders.ContentType)
                        body<ContentType.Image> {
                            mediaType(ContentType.Image.PNG)
                            description = "the response"
                        }
                    }
                }
            }) {
                val id = call.parameters["id"]?.toLong() ?: throw BadRequestException("id is null")
                val imageFile =
                    imageFileRepository.fetchImageFile(id) ?: throw NotFoundException()
                call.response.headers.append(HttpHeaders.ContentType, "image/png")
                call.respondBytes(
                    ImageFileMapper().imageFileToImageFileResponse(imageFile).data!!
                )
            }
            post({
                summary = "create imageFile."
                request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }

                    multipartBody {
                        required = true
                        description = "profile image"
                        mediaType(ContentType.MultiPart.FormData)
                        part<File>("profileImage") {
                            mediaTypes = setOf(
                                ContentType.Image.PNG,
                                ContentType.Image.JPEG,
                                ContentType.Image.GIF
                            )
                        }
//                        part<Metadata>("myMetadata")
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

                var fileDescription = ""
                var fileName = ""
                val multipartData = call.receiveMultipart()


                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            fileDescription = part.value
                        }

                        is PartData.FileItem -> {
                            fileName = part.originalFileName as String
                            val fileBytes = part.streamProvider().readBytes()
                            imageFileRepository.insertImageFile(
                                ImageFile(
                                    name = fileName, data = byteArrayToBlob(fileBytes),
                                    userId = userId
                                )
                            )
                        }

                        else -> {}
                    }
                    part.dispose()
                }
//                imageFileRepository.insertImageFile(imageFile)
//            CoroutineScope(Job()).launch { Mail().sendImageFile("hahaha") }
                call.response.status(HttpStatusCode.Created)
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
                if (imageFileRepository.fetchImageFile(id) != null) {
                    imageFileRepository.deleteImageFile(id)
                    call.respondText("Customer removed correctly", status = HttpStatusCode.Accepted)
                } else {
                    call.respondText("Not Found", status = HttpStatusCode.NotFound)
                }
            }
        }
    }
}