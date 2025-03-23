package com.example.verseloom.view

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.verseloom.BuildConfig
import com.example.verseloom.R
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import kotlinx.coroutines.launch

class AIHelpActivity : AppCompatActivity() {

    private lateinit var inputTextEditText: EditText
    private lateinit var generationButton: Button
    private lateinit var responseText: TextView
    private lateinit var generativeModel: GenerativeModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_aihelp)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inputTextEditText = findViewById(R.id.inputTextEditText)
        generationButton = findViewById(R.id.generateButton)
        responseText = findViewById(R.id.geminiResponseTextView)

        // Initialize the GenerativeModel
        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyBhFS7keIkiSLp6iqA04qd7c_UfvhCkhaQ"
        )

        // Set up the generate button click listener
        generationButton.setOnClickListener {
            val userText = inputTextEditText.text.toString().trim()
            if (userText.isNotEmpty()) {
                generateText(userText)
            } else {
                responseText.text = "Please enter a thought or idea first."
            }
        }
    }

    private fun generateText(userText: String) {
        responseText.text = "Generating inspiration..."

        // Format the prompt according to your template
        val prompt = "I'm a writer/poet and this thought came to my mind: $userText. I want to write something, inspire me also generate few lines for poets and writers."

        lifecycleScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                responseText.text = response.text ?: "No inspiration generated. Please try again."
            } catch (e: Exception) {
                responseText.text = "Error: ${e.message ?: "Unknown error occurred"}"
            }
        }
    }
}