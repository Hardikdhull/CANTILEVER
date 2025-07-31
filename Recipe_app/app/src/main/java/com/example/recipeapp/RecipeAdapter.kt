package com.example.recipeapp

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.ui.Recipe

class RecipeAdapter(
    private var recipeList: List<Recipe>,
    private val onItemClick: (Recipe) -> Unit,
    private val onEditClick: (Recipe) -> Unit,
    private val onDeleteClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.textTitle)
        val ingredientText: TextView = view.findViewById(R.id.textIngredients)
        val editBtn: ImageButton = view.findViewById(R.id.editButton)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteButton)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }
    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.titleText.text = recipe.title
        holder.ingredientText.text = recipe.ingredients

        holder.itemView.setOnClickListener { onItemClick(recipe) }
        holder.editBtn.setOnClickListener { onEditClick(recipe) }
        holder.deleteBtn.setOnClickListener { onDeleteClick(recipe) }
    }
    override fun getItemCount() = recipeList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<Recipe>) {
        recipeList = newList
        notifyDataSetChanged()
    }
}
