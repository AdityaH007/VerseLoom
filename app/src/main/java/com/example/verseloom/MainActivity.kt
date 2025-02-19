package com.example.verseloom

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth
    private  lateinit var  etEmail : EditText
    private lateinit var etPass : EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        auth = Firebase.auth
        val signup = findViewById<ImageButton>(R.id.signupbt)

        val currentUser = auth.currentUser
        if(currentUser != null)
        {
            val intent = Intent(this, LandingPage::class.java)
            startActivity(intent)
            finish()
        }

        etEmail = findViewById(R.id.etEmail)
        etPass = findViewById(R.id.etPassword)

        signup.setOnClickListener {
            signUp()
        }

    }

    private fun signUp() {
        val email = etEmail.text.toString()
        val password = etPass.text.toString()

        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this)
        {
            if(it.isSuccessful)
            {
                Toast.makeText(this, "SUCCESSFUL",Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LandingPage::class.java)
                startActivity(intent)
                finish()


            }
            else
            {
                Toast.makeText(this, "NOT SUCCESSFUL",Toast.LENGTH_SHORT).show()

            }
        }
    }
}