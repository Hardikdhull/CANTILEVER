package com.example.recipeapp.ui.Explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.RecipeAdapter
import com.example.recipeapp.RecipeDao
import com.example.recipeapp.RecipeDatabase
import com.example.recipeapp.ui.Recipe
import kotlinx.coroutines.launch

class ExploreFragment : Fragment() {

    private lateinit var recipeDao: RecipeDao
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter
    private lateinit var searchView: SearchView
    private var fullList: List<Recipe> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recipeDao = RecipeDatabase.getDatabase(requireContext()).recipeDao()
        recyclerView = view.findViewById(R.id.exploreRecyclerView)
        searchView = view.findViewById(R.id.searchView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recipeDao.getAllRecipes().observe(viewLifecycleOwner) { recipes ->
            fullList = recipes
            adapter = RecipeAdapter(
                recipeList = fullList,
                onItemClick = { recipe ->
                    val action = ExploreFragmentDirections
                        .actionNavigationExploreToRecipeDetailFragment(
                            title = recipe.title,
                            ingredients = recipe.ingredients,
                            steps = recipe.steps
                        )
                    findNavController().navigate(action)
                },
                onEditClick = { recipe ->
                },
                onDeleteClick = { recipe ->
                    lifecycleScope.launch {
                        recipeDao.deleteRecipe(recipe)
                    }
                }
            )
            recyclerView.adapter = adapter
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val filtered = fullList.filter {
                    it.title.contains(newText.orEmpty(), ignoreCase = true) ||
                            it.ingredients.contains(newText.orEmpty(), ignoreCase = true)
                }
                adapter.updateList(filtered)
                return true
            }
        })
    }
}
