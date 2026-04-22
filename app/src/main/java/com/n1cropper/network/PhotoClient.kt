package com.n1cropper.network

import android.content.Context
import com.n1cropper.device.DeviceInfo
import com.n1cropper.model.Photo
import com.n1cropper.model.PhotoList
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

class PhotoClient {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getPhotoList(device: DeviceInfo): List<Photo> {
        return try {
            val response = client.get("http://${device.host}:${device.port}/api/photos")
            if (response.status.isSuccess()) {
                response.body<PhotoList>().photos
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun uploadPhoto(device: DeviceInfo, file: File, fileName: String): Boolean {
        return try {
            val bytes = file.readBytes()
            val response = client.post("http://${device.host}:${device.port}/api/upload") {
                contentType(ContentType.MultiPart.FormData)
                setBody(bytes)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadPhotoStream(device: DeviceInfo, stream: InputStream, fileName: String): Boolean {
        return try {
            val bytes = stream.use { it.readBytes() }
            val boundary = "----WebKitFormBoundary${System.currentTimeMillis()}"
            val contentType = io.ktor.http.ContentType.parse("multipart/form-data; boundary=$boundary")

            val bodyBuilder = StringBuilder()
            bodyBuilder.append("--$boundary\r\n")
            bodyBuilder.append("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
            bodyBuilder.append("Content-Type: image/jpeg\r\n\r\n")

            val prefix = bodyBuilder.toString().toByteArray(Charsets.UTF_8)
            val suffix = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)

            val fullBody = prefix + bytes + suffix

            val response = client.post("http://${device.host}:${device.port}/api/upload") {
                this.contentType(contentType)
                setBody(fullBody)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deletePhoto(device: DeviceInfo, photoName: String): Boolean {
        return try {
            val response = client.delete(
                "http://${device.host}:${device.port}/api/photos/${photoName}"
            )
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    fun close() {
        client.close()
    }
}
