package com.example.recipeapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExploreAdapter(private val exploreList: List<ExploreRecipe>) :
    RecyclerView.Adapter<ExploreAdapter.ExploreViewHolder>() {

    class ExploreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.exploreTitle)
        val description: TextView = view.findViewById(R.id.exploreDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_explore, parent, false)
        return ExploreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        val recipe = exploreList[position]
        holder.title.text = recipe.title
        holder.description.text = recipe.description
    }

    override fun getItemCount() = exploreList.size
}
