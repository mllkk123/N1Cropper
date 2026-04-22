package com.n1cropper.model

import kotlinx.serialization.Serializable

@Serializable
data class Photo(
    val name: String,
    val size: Long,
    val timestamp: Long,
    val url: String
)

@Serializable
data class PhotoList(
    val photos: List<Photo>
)
