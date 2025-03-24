package com.example.verseloom.view

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.verseloom.Adapters.PublishedWorkAdapter
import com.example.verseloom.R
import com.example.verseloom.viewmodel.ExploreViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExploreActivity : AppCompatActivity() {

    private val viewModel: ExploreViewModel by viewModels()
    private lateinit var rvPublishedWorks: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: PublishedWorkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explore)

        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        rvPublishedWorks = findViewById(R.id.rv_published_works)
        tvEmpty = findViewById(R.id.tv_empty)

        // Set up RecyclerView
        adapter = PublishedWorkAdapter(
            mutableListOf(),
            onLikeClick = { workId, isLiked -> viewModel.likeWork(workId, isLiked) },
            onAddComment = { workId, comment -> viewModel.addComment(workId, comment) }
        )
        rvPublishedWorks.layoutManager = LinearLayoutManager(this)
        rvPublishedWorks.adapter = adapter

        // Observe published works
        viewModel.publishedWorks.observe(this) { works ->
            adapter.updateWorks(works)
            if (works.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvPublishedWorks.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvPublishedWorks.visibility = View.VISIBLE
            }
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { message ->
            message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }
}