package com.example.myapplication.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater 
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.RecipeDao
import com.example.myapplication.RecipeDatabase
import com.example.myapplication.ui.Recipe
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class AddFragment : Fragment() {

    private lateinit var recipeDao: RecipeDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val titleInput = view.findViewById<EditText>(R.id.editTextTitle)
        val ingredientsInput = view.findViewById<EditText>(R.id.editTextIngredients)
        val stepsInput = view.findViewById<EditText>(R.id.editTextSteps)
        val saveButton = view.findViewById<Button>(R.id.buttonSave)

        recipeDao = RecipeDatabase.getDatabase(requireContext()).recipeDao()

        saveButton.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val ingredients = ingredientsInput.text.toString().trim()
            val steps = stepsInput.text.toString().trim()
            if (title.isNotEmpty() && ingredients.isNotEmpty() && steps.isNotEmpty()) {
                val recipe = Recipe(title = title, ingredients = ingredients, steps = steps)
                saveRecipe(recipe)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun saveRecipe(recipe: Recipe) {
        lifecycleScope.launch {
            recipeDao.insertRecipe(recipe)
            Toast.makeText(requireContext(), "Recipe saved!", Toast.LENGTH_SHORT).show()
            requireActivity().runOnUiThread {
                requireActivity().findViewById<BottomNavigationView>(R.id.mobile_navigation)
                    .selectedItemId = R.id.navigation_home
            }
        }
    }
}
