package com.example.verseloom

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var email : EditText
    private lateinit var pass : EditText
    private lateinit var loginBtn: ImageButton
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        email = findViewById(R.id.etEmailL)
        pass = findViewById(R.id.etPasswordL)
        loginBtn = findViewById(R.id.Login)

        loginBtn.setOnClickListener {
            loginUser()
        }

    }

    private fun loginUser() {
        val emaill = email.text.toString()
        val passs = pass.text.toString()

        auth.signInWithEmailAndPassword(emaill,passs).addOnCompleteListener{ task->
            if (task.isSuccessful)
            {
                Toast.makeText(this,"Sucessful",Toast.LENGTH_SHORT).show()
                startActivity(Intent(this,LandingPage::class.java))
            }
            else
            {
                Toast.makeText(this,"UnSucessful, please try again",Toast.LENGTH_SHORT).show()
                startActivity(Intent(this,SignUpActivity::class.java))

            }
        }
    }
}