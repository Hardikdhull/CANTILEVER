package com.example.recipeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class RecipeDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_recipe_detail, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = arguments?.let {
            RecipeDetailFragmentArgs.fromBundle(it)
        }
        val title = args?.title ?: ""
        val ingredients = args?.ingredients ?: ""
        val steps = args?.steps ?: ""
        view.findViewById<TextView>(R.id.detailTitle).text = title
        view.findViewById<TextView>(R.id.detailIngredients).text = ingredients
        view.findViewById<TextView>(R.id.detailSteps).text = steps
    }
}
