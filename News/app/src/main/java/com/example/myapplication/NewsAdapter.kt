package com.example.newsaggregator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myapplication.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

class NewsAdapter(
    private val newsList: List<NewsItem>,
    private val onItemClick: (NewsItem) -> Unit
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        val sourceText: TextView = itemView.findViewById(R.id.sourceText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val newsImage: ImageView = itemView.findViewById(R.id.newsImage)
        val categoryChip: Chip = itemView.findViewById(R.id.categoryChip)
        val apiSourceChip: Chip = itemView.findViewById(R.id.apiSourceChip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.itemnews, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val newsItem = newsList[position]

        holder.titleText.text = newsItem.title
        holder.descriptionText.text = newsItem.description
        holder.sourceText.text = newsItem.source
        holder.dateText.text = newsItem.publishedAt

        // Set category chip
        holder.categoryChip.text = newsItem.category.replaceFirstChar { it.uppercase() }
        holder.categoryChip.setChipBackgroundColorResource(getCategoryColor(newsItem.category))

        // Set API source chip
        holder.apiSourceChip.text = newsItem.apiSource
        holder.apiSourceChip.setChipBackgroundColorResource(
            if (newsItem.apiSource == "NewsAPI") R.color.news_api_color else R.color.ny_times_color
        )

        // Load image with Glide
        if (newsItem.imageUrl.isNotEmpty()) {
            holder.newsImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(newsItem.imageUrl)
                .apply(RequestOptions().transform(RoundedCorners(24)))
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(holder.newsImage)
        } else {
            holder.newsImage.visibility = View.GONE
        }

        // Add ripple effect and elevation animation
        holder.cardView.setOnClickListener {
            onItemClick(newsItem)
        }

        // Add subtle animation
        holder.cardView.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(200)
            .start()
    }

    override fun getItemCount(): Int = newsList.size

    private fun getCategoryColor(category: String): Int {
        return when (category.lowercase()) {
            "business" -> R.color.category_business
            "entertainment" -> R.color.category_entertainment
            "health" -> R.color.category_health
            "science" -> R.color.category_science
            "sports" -> R.color.category_sports
            "technology" -> R.color.category_technology
            else -> R.color.category_general
        }
    }
}