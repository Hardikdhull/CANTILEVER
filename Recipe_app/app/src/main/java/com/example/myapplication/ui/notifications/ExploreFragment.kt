package com.example.myapplication.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.ExploreAdapter
import com.example.myapplication.ExploreRecipe
import com.example.myapplication.R

class ExploreFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExploreAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.exploreRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val sampleRecipes = listOf(
            ExploreRecipe("Paneer Butter Masala", "Creamy North Indian curry made with paneer, tomatoes, and spices."),
            ExploreRecipe("Spaghetti Aglio e Olio", "Quick Italian pasta with garlic and olive oil."),
            ExploreRecipe("Dosa", "South Indian rice-lentil crepe served with chutney and sambar."),
            ExploreRecipe("Veggie Fried Rice", "Stir-fried rice with fresh vegetables and soy sauce.")
        )

        adapter = ExploreAdapter(sampleRecipes)
        recyclerView.adapter = adapter
    }
}
