package com.example.recipeapp

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.recipeapp.ui.Recipe

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY id DESC")
    fun getAllRecipes(): LiveData<List<Recipe>>
    @Insert
    suspend fun insertRecipe(recipe: Recipe)
    @Delete
    suspend fun deleteRecipe(recipe: Recipe)
}
