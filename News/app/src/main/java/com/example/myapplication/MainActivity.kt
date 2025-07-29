package com.example.newsaggregator

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var chipGroup: ChipGroup
    private lateinit var progressIndicator: LinearProgressIndicator

    private val newsList = mutableListOf<NewsItem>()
    private val allNewsList = mutableListOf<NewsItem>()
    private val NEWS_API_KEY = "4ab0fc61577d4d97a4f3a642b43d408f"
    //private val NY_TIMES_API_KEY = "YOUR_NY_TIMES_API_KEY"
    private val NEWS_API_BASE = "https://newsapi.org/v2/top-headlines"
    //private val NY_TIMES_BASE = "https://api.nytimes.com/svc/topstories/v2"
    private val categories = listOf("all", "business", "entertainment", "health", "science", "sports", "technology")
    private var currentCategory = "all"
    private var currentSearchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        setupViews()
        setupRecyclerView()
        setupCategoryChips()
        loadNews()
    }
    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        chipGroup = findViewById(R.id.chipGroup)
        progressIndicator = findViewById(R.id.progressIndicator)

        swipeRefresh.setOnRefreshListener {
            loadNews()
        }

        swipeRefresh.setColorSchemeResources(
            R.color.primary_color,
            R.color.accent_color,
            R.color.secondary_color
        )
    }
    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(newsList) { newsItem ->
            openNewsInBrowser(newsItem.url)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = newsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupCategoryChips() {
        categories.forEach { category ->
            val chip = Chip(this).apply {
                text = category.replaceFirstChar { it.uppercase() }
                isCheckable = true
                isChecked = category == "all"
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        currentCategory = category
                        filterNews()
                        for (i in 0 until chipGroup.childCount) {
                            val otherChip = chipGroup.getChildAt(i) as Chip
                            if (otherChip != this) {
                                otherChip.isChecked = false
                            }
                        }
                    }
                }
            }
            chipGroup.addView(chip)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = "Search news..."
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    currentSearchQuery = it
                    searchNews(it)
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    currentSearchQuery = ""
                    filterNews()
                }
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadNews()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun loadNews() {
        if (NEWS_API_KEY == "YOUR_NEWS_API_KEY") {
            Toast.makeText(this, "Please add your API keys", Toast.LENGTH_LONG).show()
            swipeRefresh.isRefreshing = false
            return
        }
        showLoading(true)
        lifecycleScope.launch {
            try {
                allNewsList.clear()
                val newsApiData = withContext(Dispatchers.IO) {
                    fetchFromNewsAPI()
                }
//                val nyTimesData = withContext(Dispatchers.IO) {
//                    fetchFromNYTimes()
//                }
                parseNewsAPIData(newsApiData)
                //parseNYTimesData(nyTimesData)
                allNewsList.sortByDescending { it.publishedAt }
                filterNews()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error loading news: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun searchNews(query: String) {
        if (query.isEmpty()) {
            filterNews()
            return
        }
        showLoading(true)
        lifecycleScope.launch {
            try {
                allNewsList.clear()
                val newsApiData = withContext(Dispatchers.IO) {
                    searchNewsAPI(query)
                }
//                val nyTimesData = withContext(Dispatchers.IO) {
//                    searchNYTimes(query)
//                }
                parseNewsAPIData(newsApiData)
                //parseNYTimesData(nyTimesData)
                allNewsList.sortByDescending { it.publishedAt }
                filterNews()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error searching news: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun fetchFromNewsAPI(): String {
        val url = if (currentCategory == "all") {
            "$NEWS_API_BASE?country=us&pageSize=50&apiKey=$NEWS_API_KEY"
        } else {
            "$NEWS_API_BASE?country=us&category=$currentCategory&pageSize=30&apiKey=$NEWS_API_KEY"
        }
        return makeHttpRequest(url)
    }
    private fun searchNewsAPI(query: String): String {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://newsapi.org/v2/everything?q=$encodedQuery&sortBy=publishedAt&pageSize=30&apiKey=$NEWS_API_KEY"
        return makeHttpRequest(url)
    }
//    private fun fetchFromNYTimes(): String {
//        val section = if (currentCategory == "all") "home" else currentCategory
//        val url = "$NY_TIMES_BASE/$section.json?api-key=$NY_TIMES_API_KEY"
//        return makeHttpRequest(url)
//    }
//
//    private fun searchNYTimes(query: String): String {
//        val encodedQuery = URLEncoder.encode(query, "UTF-8")
//        val url = "https://api.nytimes.com/svc/search/v2/articlesearch.json?q=$encodedQuery&sort=newest&api-key=$NY_TIMES_API_KEY"
//        return makeHttpRequest(url)
//    }
    private fun makeHttpRequest(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "NewsAggregator/1.0")
            val inputStream = connection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            response.toString()
        } finally {
            connection.disconnect()
        }
    }
    private fun parseNewsAPIData(jsonResponse: String) {
        try {
            val jsonObject = JSONObject(jsonResponse)
            val articles = jsonObject.getJSONArray("articles")
            for (i in 0 until articles.length()) {
                val article = articles.getJSONObject(i)
                val newsItem = NewsItem(
                    title = article.optString("title", "No Title"),
                    description = article.optString("description", "No Description"),
                    imageUrl = article.optString("urlToImage", ""),
                    publishedAt = formatDate(article.optString("publishedAt", "")),
                    source = article.optJSONObject("source")?.optString("name", "NewsAPI") ?: "NewsAPI",
                    url = article.optString("url", ""),
                    category = determineCategory(article.optString("title", "") + " " + article.optString("description", "")),
                    apiSource = "NewsAPI"
                )
                if (newsItem.title != "No Title" && newsItem.title.isNotEmpty()) {
                    allNewsList.add(newsItem)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
//    private fun parseNYTimesData(jsonResponse: String) {
//        try {
//            val jsonObject = JSONObject(jsonResponse)
//            val articles = when {
//                jsonObject.has("results") -> jsonObject.getJSONArray("results")
//                jsonObject.has("response") -> jsonObject.getJSONObject("response").getJSONArray("docs")
//                else -> return
//            }
//
//            for (i in 0 until articles.length()) {
//                val article = articles.getJSONObject(i)
//
//                val newsItem = if (jsonObject.has("response")) {
//                    NewsItem(
//                        title = article.optString("headline", "").let {
//                            if (it.isEmpty()) article.optJSONObject("headline")?.optString("main", "No Title") ?: "No Title"
//                            else it
//                        },
//                        description = article.optString("abstract", "No Description"),
//                        imageUrl = getImageFromNYTimes(article),
//                        publishedAt = formatNYTimesDate(article.optString("pub_date", "")),
//                        source = "The New York Times",
//                        url = article.optString("web_url", ""),
//                        category = determineCategory(article.optString("section_name", "")),
//                        apiSource = "NY Times"
//                    )
//                } else {
//                    NewsItem(
//                        title = article.optString("title", "No Title"),
//                        description = article.optString("abstract", "No Description"),
//                        imageUrl = getImageFromNYTimes(article),
//                        publishedAt = formatNYTimesDate(article.optString("published_date", "")),
//                        source = "The New York Times",
//                        url = article.optString("url", ""),
//                        category = article.optString("section", "general"),
//                        apiSource = "NY Times"
//                    )
//                }
//                if (newsItem.title != "No Title" && newsItem.title.isNotEmpty()) {
//                    allNewsList.add(newsItem)
//                }
//            }
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    private fun getImageFromNYTimes(article: JSONObject): String {
//        return try {
//            val multimedia = article.optJSONArray("multimedia")
//            if (multimedia != null && multimedia.length() > 0) {
//                val image = multimedia.getJSONObject(0)
//                val url = image.optString("url", "")
//                if (url.startsWith("http")) url else "https://www.nytimes.com/$url"
//            } else ""
//        } catch (e: Exception) {
//            ""
//        }
//    }
    private fun determineCategory(text: String): String {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("business") || lowerText.contains("economy") || lowerText.contains("market") -> "business"
            lowerText.contains("health") || lowerText.contains("medical") || lowerText.contains("covid") -> "health"
            lowerText.contains("tech") || lowerText.contains("ai") || lowerText.contains("computer") -> "technology"
            lowerText.contains("sport") || lowerText.contains("football") || lowerText.contains("basketball") -> "sports"
            lowerText.contains("science") || lowerText.contains("research") || lowerText.contains("study") -> "science"
            lowerText.contains("entertainment") || lowerText.contains("movie") || lowerText.contains("music") -> "entertainment"
            else -> "general"
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun filterNews() {
        newsList.clear()
        val filteredList = if (currentCategory == "all") {
            allNewsList
        } else {
            allNewsList.filter { it.category == currentCategory }
        }
        newsList.addAll(filteredList)
        newsAdapter.notifyDataSetChanged()

        // Scroll to top
        recyclerView.scrollToPosition(0)
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

//    private fun formatNYTimesDate(dateString: String): String {
//        return try {
//            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
//            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
//            val date = inputFormat.parse(dateString)
//            outputFormat.format(date ?: Date())
//        } catch (e: Exception) {
//            try {
//                val altFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                val date = altFormat.parse(dateString)
//                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
//                outputFormat.format(date ?: Date())
//            } catch (e2: Exception) {
//                dateString
//            }
//        }
//    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressIndicator.show()
        } else {
            progressIndicator.hide()
            swipeRefresh.isRefreshing = false
        }
    }

    private fun openNewsInBrowser(url: String) {
        if (url.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }
}
data class NewsItem(
    val title: String,
    val description: String,
    val imageUrl: String,
    val publishedAt: String,
    val source: String,
    val url: String,
    val category: String = "general",
    val apiSource: String = ""
)