package com.n1cropper.network

import android.content.Context
import android.os.Environment
import com.n1cropper.crop.CropEngine
import com.n1cropper.model.Photo
import com.n1cropper.model.PhotoList
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class PhotoServer(private val context: Context) {

    private val photosDir = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "N1Cropper"
    ).apply { mkdirs() }

    private var server: ApplicationEngine? = null

    private val _photoList = MutableStateFlow<List<Photo>>(emptyList())
    val photoList: StateFlow<List<Photo>> = _photoList.asStateFlow()

    val port: Int
        get() = server?.environment?.connectors?.firstOrNull()?.port ?: 8080

    fun start() {
        if (server != null) return

        refreshPhotoList()

        server = embeddedServer(Netty, port = 0) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            routing {
                // 获取照片列表
                get("/api/photos") {
                    val list = getPhotoList()
                    call.respond(PhotoList(list))
                }

                // 获取单张照片
                get("/api/photos/{name}") {
                    val name = call.parameters["name"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest
                    )
                    val file = File(photosDir, name)
                    if (!file.exists()) {
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        call.respondFile(file)
                    }
                }

                // 上传照片
                post("/api/upload") {
                    val multipart = call.receiveMultipart()
                    var savedFile: File? = null

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val originalName = part.originalFileName ?: "unknown.jpg"
                                val ext = originalName.substringAfterLast(".", "jpg")
                                val newName = "${UUID.randomUUID()}.$ext"
                                val file = File(photosDir, newName)
                                part.streamProvider().use { input ->
                                    file.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                CropEngine.cropTo43(file)
                                savedFile = file
                            }
                            else -> {}
                        }
                        part.dispose()
                    }

                    if (savedFile != null) {
                        refreshPhotoList()
                        call.respond(mapOf("success" to true, "filename" to savedFile!!.name))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("success" to false))
                    }
                }

                // 删除照片
                delete("/api/photos/{name}") {
                    val name = call.parameters["name"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest
                    )
                    val file = File(photosDir, name)
                    if (file.exists()) {
                        file.delete()
                        refreshPhotoList()
                        call.respond(mapOf("success" to true))
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }.start(wait = false)

        refreshPhotoList()
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    fun deletePhoto(name: String): Boolean {
        val file = File(photosDir, name)
        return if (file.exists()) {
            file.delete().also { if (it) refreshPhotoList() }
        } else false
    }

    fun getPhotosDir(): File = photosDir

    private fun refreshPhotoList() {
        _photoList.value = getPhotoList()
    }

    private fun getPhotoList(): List<Photo> {
        if (!photosDir.exists()) return emptyList()
        return photosDir.listFiles { f ->
            f.extension.equals("jpg", ignoreCase = true) ||
            f.extension.equals("jpeg", ignoreCase = true) ||
            f.extension.equals("png", ignoreCase = true)
        }?.sortedByDescending { it.lastModified() }?.map { file ->
            Photo(
                name = file.name,
                size = file.length(),
                timestamp = file.lastModified(),
                url = file.absolutePath
            )
        } ?: emptyList()
    }
}
