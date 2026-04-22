package com.n1cropper

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        findViewById<MaterialCardView>(R.id.cardServer).setOnClickListener {
            startActivity(Intent(this, ServerActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardClient).setOnClickListener {
            startActivity(Intent(this, ClientActivity::class.java))
        }
    }
}
