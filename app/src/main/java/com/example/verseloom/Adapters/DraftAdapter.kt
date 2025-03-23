package com.example.verseloom.Adapters

import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.verseloom.R
import com.example.verseloom.database.Draft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DraftAdapter(context: Context) : BaseAdapter() {

    private val drafts = mutableListOf<Draft>()
    private val inflater = LayoutInflater.from(context)

    fun setDrafts(newDrafts: List<Draft>) {
        drafts.clear()
        drafts.addAll(newDrafts)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = drafts.size

    override fun getItem(position: Int): Draft = drafts[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_draft, parent, false)

        val draft = getItem(position)

        val tvContent = view.findViewById<TextView>(R.id.tv_content)
        val tvTimestamp = view.findViewById<TextView>(R.id.tv_timestamp)

        // Render HTML content
        tvContent.text = Html.fromHtml(draft.content, Html.FROM_HTML_MODE_COMPACT)
        tvTimestamp.text = "Saved: ${formatTimestamp(draft.timestamp)}"

        return view
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
}