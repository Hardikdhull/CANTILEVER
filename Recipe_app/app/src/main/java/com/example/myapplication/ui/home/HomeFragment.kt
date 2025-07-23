package com.example.myapplication.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.RecipeAdapter
import com.example.myapplication.RecipeDao
import com.example.myapplication.RecipeDatabase

class HomeFragment : Fragment() {

    private lateinit var recipeDao: RecipeDao
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

        recipeDao = RecipeDatabase.getDatabase(requireContext()).recipeDao()
        recyclerView = view.findViewById(R.id.recipeRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe LiveData and update UI
        recipeDao.getAllRecipes().observe(viewLifecycleOwner) { recipeList ->
            adapter = RecipeAdapter(recipeList)
            recyclerView.adapter = adapter
        }
    }
}
