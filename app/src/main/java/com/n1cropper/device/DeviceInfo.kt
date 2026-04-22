package com.n1cropper.device

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val name: String,
    val host: String,
    val port: Int,
    val photoCount: Int = 0
)
