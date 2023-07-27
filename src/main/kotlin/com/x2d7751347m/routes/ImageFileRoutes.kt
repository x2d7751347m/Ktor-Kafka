package com.x2d7751347m.routes

import com.x2d7751347m.dto.*
import com.x2d7751347m.entity.ImageFile
import com.x2d7751347m.plugins.ExceptionResponse
import com.x2d7751347m.plugins.ValidationExceptions
import com.x2d7751347m.repository.ImageFileRepository
import io.github.smiley4.ktorswaggerui.dsl.*
import io.ktor.http.*
import io.ktor.http.cio.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.r2dbc.spi.Blob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.collect
import kotlinx.serialization.builtins.serializer
import org.mapstruct.factory.Mappers
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

data class Metadata(
    val format: String,
    val location: Coords
)
data class Coords(
    val lat: Float,
    val long: Float
)
fun blobToByteArray(blob: Blob): ByteArray {
    val initialByteBuffer = ByteBuffer.allocate(0)

    return Flux.from(blob.stream())
        .concatMap { buffer -> Flux.just(buffer) }
        .reduce(initialByteBuffer) { acc, buffer ->
            val combined = ByteBuffer.allocate(acc.remaining() + buffer.remaining())
            combined.put(acc).put(buffer)
            combined.flip()
            combined
        }
        .map { buffer ->
            val byteArray = ByteArray(buffer.remaining())
            buffer.get(byteArray)
            byteArray
        }.block()!!
}

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
            val imageFileData =
                imageFileRepository.fetchImageFile(id)?.data ?: throw NotFoundException()
            call.response.headers.append(HttpHeaders.ContentType, "image/png")
            call.respondBytes(
                blobToByteArray(imageFileData)
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
                    imageFileRepository.fetchImageFilesByUserId(page, size, userId).toList()
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
                        part<Metadata>("myMetadata")
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
                            imageFileRepository.insertImageFile(ImageFile(name = fileName, data = Blob.from( Mono.just(ByteBuffer.wrap(fileBytes)) ),
                                userId = userId))
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
                call.respond(imageFileRepository.fetchImageFiles(page, size))
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
                val imageFile =
                    imageFileRepository.fetchImageFile(id) ?: throw NotFoundException()
                call.respond(imageFile)
            }
            post({
                summary = "create imageFile."
                request {
//                pathParameter<String>("operation") {
//                    description = "the math operation to perform. Either 'add' or 'sub'"
//                    example = "add"
//                }
                    multipartBody {
                        mediaType(ContentType.Application.OctetStream)
                        required = true
                        description = "file"
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

                val imageFile = call.receive<ContentType.MultiPart>()
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
                if (imageFileRepository.fetchImageFile(id)!=null) {
                    imageFileRepository.deleteImageFile(id)
                    call.respondText("Customer removed correctly", status = HttpStatusCode.Accepted)
                } else {
                    call.respondText("Not Found", status = HttpStatusCode.NotFound)
                }
            }
        }
    }
}