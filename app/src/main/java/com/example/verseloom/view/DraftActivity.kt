package com.example.verseloom.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.verseloom.Adapters.DraftAdapter
import com.example.verseloom.DraftsViewModel
import com.example.verseloom.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DraftActivity : AppCompatActivity() {

    private val viewModel: DraftsViewModel by viewModels()
    private lateinit var lvDrafts: ListView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: DraftAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft)

        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        lvDrafts = findViewById(R.id.lv_drafts)
        tvEmpty = findViewById(R.id.tv_empty)

        // Set up the adapter
        adapter = DraftAdapter(this)
        lvDrafts.adapter = adapter

        // Observe drafts
        viewModel.drafts.observe(this) { drafts ->
            adapter.setDrafts(drafts)

            // Show/hide empty state
            if (drafts.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                lvDrafts.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                lvDrafts.visibility = View.VISIBLE
            }
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { message ->
            message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }

        // Add click listener to edit drafts
        lvDrafts.setOnItemClickListener { _, _, position, _ ->
            val draft = adapter.getItem(position) ?: return@setOnItemClickListener
            // Navigate to WriteActivity with the draft content
            val intent = Intent(this, WriteActivity::class.java).apply {
                putExtra("DRAFT_CONTENT", draft.content)
            }
            startActivity(intent)
        }
    }
}