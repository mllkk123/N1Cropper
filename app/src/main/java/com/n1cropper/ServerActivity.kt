package com.n1cropper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.n1cropper.crop.CropEngine
import com.n1cropper.device.DeviceDiscovery
import com.n1cropper.model.Photo
import com.n1cropper.network.PhotoServer
import com.n1cropper.ui.adapter.PhotoAdapter
import kotlinx.coroutines.launch

class ServerActivity : AppCompatActivity() {

    companion object {
        const val PERMISSION_REQUEST = 100
    }

    private lateinit var photoServer: PhotoServer
    private lateinit var deviceDiscovery: DeviceDiscovery
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvStatus: TextView
    private lateinit var tvPort: TextView
    private lateinit var fabDelete: FloatingActionButton

    private var selectedPhotos = mutableSetOf<String>()
    private var isDeleteMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        recyclerView = findViewById(R.id.recyclerView)
        tvStatus = findViewById(R.id.tvStatus)
        tvPort = findViewById(R.id.tvPort)
        fabDelete = findViewById(R.id.fabDelete)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST
            )
        } else {
            initServer()
        }

        fabDelete.setOnClickListener {
            if (isDeleteMode && selectedPhotos.isNotEmpty()) {
                confirmDelete()
            } else {
                toggleDeleteMode()
            }
        }
    }

    private fun initServer() {
        photoServer = PhotoServer(this)
        deviceDiscovery = DeviceDiscovery(this)

        photoServer.start()
        val port = photoServer.port
        deviceDiscovery.startServer(port)

        tvStatus.text = "服务器运行中"
        tvPort.text = "端口: $port"

        photoAdapter = PhotoAdapter(
            onItemClick = { photo ->
                if (isDeleteMode) {
                    toggleSelection(photo)
                }
            },
            onItemLongClick = { photo ->
                if (!isDeleteMode) {
                    toggleDeleteMode()
                    toggleSelection(photo)
                }
                true
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = photoAdapter

        lifecycleScope.launch {
            photoServer.photoList.collect { photos ->
                photoAdapter.submitList(photos)
            }
        }
    }

    private fun toggleDeleteMode() {
        isDeleteMode = !isDeleteMode
        selectedPhotos.clear()
        photoAdapter.setSelectionMode(isDeleteMode)
        fabDelete.setImageResource(
            if (isDeleteMode) android.R.drawable.ic_menu_delete else android.R.drawable.ic_menu_close
        )
    }

    private fun toggleSelection(photo: Photo) {
        if (selectedPhotos.contains(photo.name)) {
            selectedPhotos.remove(photo.name)
        } else {
            selectedPhotos.add(photo.name)
        }
        photoAdapter.setSelected(selectedPhotos)
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定删除选中的 ${selectedPhotos.size} 张照片？")
            .setPositiveButton("删除") { _, _ ->
                selectedPhotos.forEach { name ->
                    photoServer.deletePhoto(name)
                }
                toggleDeleteMode()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initServer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        photoServer.stop()
        deviceDiscovery.stopServer()
    }
}
