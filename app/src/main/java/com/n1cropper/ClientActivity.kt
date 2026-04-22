package com.n1cropper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.n1cropper.device.DeviceDiscovery
import com.n1cropper.device.DeviceInfo
import com.n1cropper.model.Photo
import com.n1cropper.network.PhotoClient
import com.n1cropper.ui.adapter.DeviceAdapter
import com.n1cropper.ui.adapter.PhotoAdapter
import kotlinx.coroutines.launch

class ClientActivity : AppCompatActivity() {

    companion object {
        const val PICK_IMAGES = 1001
    }

    private lateinit var deviceDiscovery: DeviceDiscovery
    private lateinit var photoClient: PhotoClient
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var recyclerViewPhotos: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var fabUpload: FloatingActionButton
    private lateinit var btnRefresh: MaterialButton

    private var connectedDevice: DeviceInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        recyclerViewDevices = findViewById(R.id.recyclerViewDevices)
        recyclerViewPhotos = findViewById(R.id.recyclerViewPhotos)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        fabUpload = findViewById(R.id.fabUpload)
        btnRefresh = findViewById(R.id.btnRefresh)

        photoClient = PhotoClient()

        deviceAdapter = DeviceAdapter { device ->
            connectToDevice(device)
        }
        recyclerViewDevices.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerViewDevices.adapter = deviceAdapter

        photoAdapter = PhotoAdapter(
            onItemClick = { photo ->
                // 预览
            },
            onItemLongClick = { photo ->
                deletePhoto(photo)
                true
            },
            baseUrl = ""
        )
        recyclerViewPhotos.layoutManager = GridLayoutManager(this, 3)
        recyclerViewPhotos.adapter = photoAdapter

        deviceDiscovery = DeviceDiscovery(this)
        deviceDiscovery.startDiscovery()

        lifecycleScope.launch {
            deviceDiscovery.discoveredDevices.collect { devices ->
                deviceAdapter.submitList(devices)
                if (devices.isEmpty()) {
                    tvStatus.text = "正在搜索设备..."
                }
            }
        }

        btnRefresh.setOnClickListener {
            deviceDiscovery.stopDiscovery()
            deviceDiscovery.startDiscovery()
            tvStatus.text = "正在搜索设备..."
        }

        fabUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(Intent.createChooser(intent, "选择图片"), PICK_IMAGES)
        }

        fabUpload.visibility = View.GONE
    }

    private fun connectToDevice(device: DeviceInfo) {
        connectedDevice = device
        photoAdapter.setBaseUrl("http://${device.host}:${device.port}")
        tvStatus.text = "已连接: ${device.name}"
        fabUpload.visibility = View.VISIBLE
        recyclerViewDevices.visibility = View.GONE
        recyclerViewPhotos.visibility = View.VISIBLE
        btnRefresh.text = "断开连接"
        btnRefresh.setOnClickListener {
            disconnect()
        }
        loadPhotos()
    }

    private fun disconnect() {
        connectedDevice = null
        photoAdapter.submitList(emptyList())
        fabUpload.visibility = View.GONE
        recyclerViewDevices.visibility = View.VISIBLE
        recyclerViewPhotos.visibility = View.GONE
        btnRefresh.text = "重新搜索"
        btnRefresh.setOnClickListener {
            deviceDiscovery.stopDiscovery()
            deviceDiscovery.startDiscovery()
        }
        tvStatus.text = "已断开"
    }

    private fun loadPhotos() {
        val device = connectedDevice ?: return
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            val photos = photoClient.getPhotoList(device)
            photoAdapter.submitList(photos)
            progressBar.visibility = View.GONE
            tvStatus.text = "已连接: ${device.name} (${photos.size} 张照片)"
        }
    }

    private fun deletePhoto(photo: Photo) {
        val device = connectedDevice ?: return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除照片")
            .setMessage("确定删除这张照片？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    photoClient.deletePhoto(device, photo.name)
                    loadPhotos()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES && resultCode == Activity.RESULT_OK) {
            val device = connectedDevice ?: return
            val uris = mutableListOf<Uri>()

            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } ?: data?.data?.let { uris.add(it) }

            if (uris.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE
                tvStatus.text = "正在上传 ${uris.size} 张照片..."

                lifecycleScope.launch {
                    var success = 0
                    uris.forEach { uri ->
                        contentResolver.openInputStream(uri)?.use { stream ->
                            val fileName = uri.lastPathSegment ?: "image.jpg"
                            if (photoClient.uploadPhotoStream(device, stream, fileName)) {
                                success++
                            }
                        }
                    }
                    tvStatus.text = "上传完成: $success/${uris.size} 成功"
                    loadPhotos()
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceDiscovery.stopDiscovery()
        photoClient.close()
    }
}
