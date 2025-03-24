package com.example.verseloom.Adapters

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.verseloom.R
import com.example.verseloom.database.PublishedWork
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PublishedWorkAdapter(
    private val works: MutableList<PublishedWork>,
    private val onLikeClick: (String, Boolean) -> Unit,
    private val onAddComment: (String, String) -> Unit
) : RecyclerView.Adapter<PublishedWorkAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        val btnLike: ImageButton = itemView.findViewById(R.id.btn_like)
        val tvLikes: TextView = itemView.findViewById(R.id.tv_likes)
        val llCommentList: LinearLayout = itemView.findViewById(R.id.ll_comment_list)
        val etComment: EditText = itemView.findViewById(R.id.et_comment)
        val btnAddComment: Button = itemView.findViewById(R.id.btn_add_comment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_published_work, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val work = works[position]

        holder.tvUsername.text = work.userName
        holder.tvTimestamp.text = "Posted: ${formatTimestamp(work.timestamp)}"
        holder.tvContent.text = Html.fromHtml(work.content, Html.FROM_HTML_MODE_COMPACT)
        holder.tvLikes.text = "${work.likes} Likes"

        // Set like button state
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val isLiked = currentUserId != null && work.likedBy.contains(currentUserId)
        holder.btnLike.setImageResource(
            if (isLiked) android.R.drawable.star_on else android.R.drawable.star_off
        )

        holder.btnLike.setOnClickListener {
            onLikeClick(work.id, isLiked)
        }

        // Display comments
        holder.llCommentList.removeAllViews()
        work.comments.forEach { comment ->
            val commentView = TextView(holder.itemView.context).apply {
                text = comment
                textSize = 14f
                setPadding(0, 4, 0, 4)
            }
            holder.llCommentList.addView(commentView)
        }

        // Add comment
        holder.btnAddComment.setOnClickListener {
            val comment = holder.etComment.text.toString()
            if (comment.isNotBlank()) {
                onAddComment(work.id, comment)
                holder.etComment.text.clear()
            }
        }
    }

    override fun getItemCount(): Int = works.size

    fun updateWorks(newWorks: List<PublishedWork>) {
        works.clear()
        works.addAll(newWorks)
        notifyDataSetChanged()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
}