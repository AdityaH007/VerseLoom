package com.example.verseloom

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Animation for Logo
        val fadeInAnimation = AlphaAnimation(0f,1f).apply {
            duration = 2000 //2 second
            fillAfter = true
        }

        val logo = findViewById<ImageView>(R.id.logoSS)

        logo.startAnimation(fadeInAnimation)

        //Delay and Transition
        Handler(Looper.getMainLooper()).postDelayed(
            {
                val intent = Intent(this,MainActivity::class.java)
                startActivity(intent)

                finish() //close splash activity
            }, 2500
        )
    }
}