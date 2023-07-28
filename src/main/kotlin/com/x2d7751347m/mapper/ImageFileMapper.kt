package com.x2d7751347m.mapper

import com.x2d7751347m.dto.ImageFileResponse
import com.x2d7751347m.entity.ImageFile
import com.x2d7751347m.plugins.ImageUtil
import io.r2dbc.spi.Blob
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

class ImageFileMapper {

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

    fun byteArrayToBlob(byteArray: ByteArray): Blob =
        Blob.from(Mono.just(ByteBuffer.wrap(ImageUtil.compressImage(byteArray))))

    fun imageFileToImageFileResponse(imageFile: ImageFile): ImageFileResponse = ImageFileResponse(
        id = imageFile.id,
        name = imageFile.name,
        type = imageFile.type,
        data = ImageUtil.decompressImage(blobToByteArray(imageFile.data!!)),
        userId = imageFile.userId,
    )

    fun imageFileListToImageFileResponseList(imageFileList: List<ImageFile>): List<ImageFileResponse> =
        imageFileList.map { imageFileToImageFileResponse(it) }
}