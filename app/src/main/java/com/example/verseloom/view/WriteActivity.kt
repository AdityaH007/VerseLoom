package com.example.verseloom.view

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import android.text.Html
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.verseloom.R
import com.example.verseloom.viewmodel.WriteViewModel

@AndroidEntryPoint
class WriteActivity : AppCompatActivity() {

    private val viewModel: WriteViewModel by viewModels()
    private lateinit var etContent: EditText
    private lateinit var tvWordCount: TextView
    private lateinit var btnBold: Button
    private lateinit var btnItalics: Button
    private lateinit var btnUnderline: Button
    private lateinit var spinnerFontSize: Spinner
    private lateinit var spinnerAlignment: Spinner
    private lateinit var btnBackground: Button
    private lateinit var btnSave: Button
    private lateinit var btnPublish: Button
    private lateinit var writeContainer: ConstraintLayout
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // Initialize views
        etContent = findViewById(R.id.et_write_content)
        tvWordCount = findViewById(R.id.tv_word_count)
        btnBold = findViewById(R.id.btn_bold)
        btnItalics = findViewById(R.id.btn_italics)
        btnUnderline = findViewById(R.id.btn_underline)
        spinnerFontSize = findViewById(R.id.spinner_font_size)
        spinnerAlignment = findViewById(R.id.spinner_alignment)
        btnBackground = findViewById(R.id.btn_background)
        btnSave = findViewById(R.id.btn_save)
        btnPublish = findViewById(R.id.btn_publish)
        writeContainer = findViewById(R.id.main)

        // Check for draft content to load
        intent.getStringExtra("DRAFT_CONTENT")?.let { draftContent ->
            viewModel.loadDraftContent(draftContent)
            // Pass the draft ID and Firestore document ID if editing
            val writingId = intent.getLongExtra("WRITING_ID", -1).takeIf { it != -1L }
            val firestoreDocId = intent.getStringExtra("FIRESTORE_DOC_ID")
            viewModel.setCurrentWritingId(writingId, firestoreDocId)
        }

        // Observe ViewModel data
        viewModel.content.observe(this) { content ->
            if (content != null) {
                // Render HTML content in EditText
                val spannable = Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT)
                if (etContent.text.toString() != spannable.toString()) {
                    etContent.setText(spannable)
                }
            } else {
                etContent.text = null
            }
        }

        viewModel.wordCount.observe(this) { count ->
            tvWordCount.text = "Words: $count"
        }

        viewModel.fontSize.observe(this) { size ->
            etContent.textSize = size
        }

        viewModel.alignment.observe(this) { gravity ->
            etContent.gravity = gravity or Gravity.TOP
        }

        viewModel.backgroundColor.observe(this) { colorRes ->
            colorRes?.let { writeContainer.setBackgroundColor(ContextCompat.getColor(this, it)) }
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }

        // Set up listeners
        etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Convert the Spannable content to HTML and update the ViewModel
                val htmlContent = Html.toHtml(s, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                viewModel.updateContent(htmlContent)
            }
        })

        btnBold.setOnClickListener { applyStyle(StyleSpan(android.graphics.Typeface.BOLD)) }
        btnItalics.setOnClickListener { applyStyle(StyleSpan(android.graphics.Typeface.ITALIC)) }
        btnUnderline.setOnClickListener { applyStyle(UnderlineSpan()) }

        spinnerFontSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setFontSize(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerAlignment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setAlignment(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnBackground.setOnClickListener { showBackgroundColorDialog() }
        btnSave.setOnClickListener { viewModel.saveContent() }
        btnPublish.setOnClickListener {
            val userName = sharedPreferences.getString("USER_NAME", "Anonymous") ?: "Anonymous"
            viewModel.publishContent(userName)
        }
    }

    private fun applyStyle(span: Any) {
        val start = etContent.selectionStart
        val end = etContent.selectionEnd

        if (start == end || start < 0 || end < 0) {
            Toast.makeText(this, "Please select text to apply formatting", Toast.LENGTH_SHORT).show()
            return
        }

        val spannable = etContent.text as Spannable
        spannable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun showBackgroundColorDialog() {
        val colors = arrayOf(
            "White" to R.color.white,
            "Light Gray" to R.color.light_gray,
            "Light Yellow" to R.color.light_yellow,
            "Light Blue" to R.color.light_blue
        )
        val colorNames = colors.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choose Background Color")
            .setItems(colorNames) { _, which ->
                viewModel.setBackgroundColor(colors[which].second)
            }
            .show()
    }

    override fun onPause() {
        super.onPause()
        // Auto-save on pause
        viewModel.saveContent(isAutoSave = true)
    }
}