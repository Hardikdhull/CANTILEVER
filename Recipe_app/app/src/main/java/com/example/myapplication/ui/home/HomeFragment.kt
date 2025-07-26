package com.example.myapplication.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.RecipeAdapter
import com.example.myapplication.RecipeDatabase
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var adapter: RecipeAdapter
    private lateinit var recyclerView: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recipeRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val recipeDao = RecipeDatabase.getDatabase(requireContext()).recipeDao()
        recipeDao.getAllRecipes().observe(viewLifecycleOwner) { recipeList ->
            adapter = RecipeAdapter(
                recipeList,
                onItemClick = { selectedRecipe ->
                    val action = HomeFragmentDirections.actionNavigationHomeToRecipeDetailFragment(
                        title = selectedRecipe.title,
                        ingredients = selectedRecipe.ingredients,
                        steps = selectedRecipe.steps
                    )
                    findNavController().navigate(action)
                },
                onEditClick = { recipe ->
                    Toast.makeText(requireContext(), "Edit ${recipe.title}", Toast.LENGTH_SHORT).show()
                },
                onDeleteClick = { recipe ->
                    Thread {
                        lifecycleScope.launch {
                            recipeDao.deleteRecipe(recipe)
                        }

                    }.start()
                }
            )
            recyclerView.adapter = adapter
        }
    }
}
