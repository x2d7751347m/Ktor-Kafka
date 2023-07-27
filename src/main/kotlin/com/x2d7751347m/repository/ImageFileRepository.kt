package com.x2d7751347m.repository

import com.x2d7751347m.entity.ImageFile
import com.x2d7751347m.entity.ImageFileData
import com.x2d7751347m.entity.imageFile
import com.x2d7751347m.r2dbcDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.firstOrNull
import java.nio.ByteBuffer
import java.sql.Blob

class ImageFileRepository {

    val imageFileDef = Meta.imageFile
    val db = r2dbcDatabase
    suspend fun insertImageFile(imageFile: ImageFile): ImageFile {
        db.runQuery {
            QueryDsl.insert(imageFileDef).single(imageFile)
        }
        return db.runQuery {
            QueryDsl.from(imageFileDef).where { imageFileDef.name eq imageFile.name }
        }.last()
    }

    suspend fun updateImageFile(imageFileData: ImageFileData): ImageFile {
        db.runQuery {
            QueryDsl.update(imageFileDef)
                .set {
                    imageFileData.name?.run { imageFileDef.name eq this }
                    imageFileData.userId?.run { imageFileDef.userId eq this }
                    imageFileData.type?.run { imageFileDef.type eq this }
                    imageFileData.data?.run { imageFileDef.data eq this }
                }
                .where { imageFileDef.id eq imageFileData.id }
        }
        return db.flowQuery {
            QueryDsl.from(imageFileDef).where { imageFileDef.id eq imageFileData.id }
        }.last()
    }

    suspend fun fetchImageFiles(page: Int, size: Int): Flow<ImageFile> {

        // SELECT
        val imageFile = db.flowQuery {
            QueryDsl.from(imageFileDef).offset((page - 1).times(size)).limit(size)
        }
        return imageFile
    }

    suspend fun fetchImageFilesByUserId(page: Int, size: Int, userId: Long): Flow<ImageFile> {

        // SELECT
        val imageFile = db.flowQuery {
            QueryDsl.from(imageFileDef).where { imageFileDef.userId eq userId }.offset((page - 1).times(size)).limit(size)
        }
        return imageFile
    }

    suspend fun fetchImageFile(id: Long): ImageFile? {

        // SELECT
        val imageFile = db.runQuery {
            QueryDsl.from(imageFileDef).where { imageFileDef.id eq id }.firstOrNull()
        }
        return imageFile
    }

    suspend fun fetchAllImageFilesByUserId(userId: Long): List<ImageFile> {

        // SELECT
        val imageFileList = db.runQuery {
            QueryDsl.from(imageFileDef).where { imageFileDef.userId eq userId }
        }
        return imageFileList
    }

    suspend fun deleteImageFile(id: Long) {
        db.runQuery { QueryDsl.delete(imageFileDef).where { imageFileDef.id eq id } }
    }

    suspend fun deleteAllImageFilesByUserId(userId: Long) {
        db.runQuery { QueryDsl.delete(imageFileDef).where { imageFileDef.userId eq userId } }
    }
}