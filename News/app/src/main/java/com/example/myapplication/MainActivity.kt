package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.SSLException

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var chipGroup: ChipGroup
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var networkManager: NetworkManager
    private val newsList = mutableListOf<NewsItem>()
    private val allNewsList = mutableListOf<NewsItem>()
    private val NEWS_API_KEY = "4ab0fc61577d4d97a4f3a642b43d408f"
    private val NEWS_API_BASE = "https://newsapi.org/v2/top-headlines"
    private val categories = listOf("all", "business", "entertainment", "health", "science", "sports", "technology")
    private var currentCategory = "all"
    private var currentSearchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        networkManager = NetworkManager.getInstance(this)
        setupViews()
        setupRecyclerView()
        setupCategoryChips()
        setupNetworkObserver()
        if (networkManager.isNetworkAvailable()) {
            loadNews()
        } else {
            showNetworkError()
        }
    }
    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        chipGroup = findViewById(R.id.chipGroup)
        progressIndicator = findViewById(R.id.progressIndicator)
        swipeRefresh.setOnRefreshListener {
            if (networkManager.isNetworkAvailable()) {
                loadNews()
            } else {
                swipeRefresh.isRefreshing = false
                showNetworkError()
            }
        }
        swipeRefresh.setColorSchemeResources(
            R.color.primary_color,
            R.color.accent_color,
            R.color.secondary_color
        )
    }
    private fun setupNetworkObserver() {
        networkManager.networkStatus.observe(this) { status ->
            when (status) {
                NetworkStatus.CONNECTED -> {
                    if (allNewsList.isEmpty()) {
                        loadNews()
                    }
                    showConnectionStatus("Connected", false)
                }
                NetworkStatus.DISCONNECTED -> {
                    showConnectionStatus("No internet connection", true)
                }
            }
        }
    }
    private fun showConnectionStatus(message: String, isError: Boolean) {
        val rootView = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, message,
            if (isError) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_SHORT)
        if (isError) {
            snackbar.setAction("Retry") {
                if (networkManager.isNetworkAvailable()) {
                    loadNews()
                }
            }
        }
        snackbar.show()
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
                    if (networkManager.isNetworkAvailable()) {
                        currentSearchQuery = it
                        searchNews(it)
                    } else {
                        showNetworkError()
                    }
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
                if (networkManager.isNetworkAvailable()) {
                    loadNews()
                } else {
                    showNetworkError()
                }
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
        if (!networkManager.isNetworkAvailable()) {
            showNetworkError()
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            try {
                allNewsList.clear()
                val newsApiData = withContext(Dispatchers.IO) {
                    fetchFromNewsAPI()
                }
                parseNewsAPIData(newsApiData)
                allNewsList.sortByDescending { it.publishedAt }
                filterNews()
                if (allNewsList.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No news articles found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                handleNetworkError(e)
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
        if (!networkManager.isNetworkAvailable()) {
            showNetworkError()
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            try {
                allNewsList.clear()
                val newsApiData = withContext(Dispatchers.IO) {
                    searchNewsAPI(query)
                }
                parseNewsAPIData(newsApiData)
                allNewsList.sortByDescending { it.publishedAt }
                filterNews()
                if (allNewsList.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No results found for '$query'", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                handleNetworkError(e)
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

    private fun makeHttpRequest(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "NewsAggregator/1.0")
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP Error: $responseCode")
            }
            val inputStream = connection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            response.toString()
        } catch (e: Exception) {
            throw e
        } finally {
            connection.disconnect()
        }
    }

    private fun parseNewsAPIData(jsonResponse: String) {
        try {
            val jsonObject = JSONObject(jsonResponse)
            if (jsonObject.has("status") && jsonObject.getString("status") == "error") {
                val errorMessage = jsonObject.optString("message", "Unknown API error")
                throw Exception("API Error: $errorMessage")
            }

            val articles = jsonObject.getJSONArray("articles")
            for (i in 0 until articles.length()) {
                val article = articles.getJSONObject(i)
                val title = article.optString("title", "")
                val description = article.optString("description", "")
                if (title.isNotEmpty() && title != "null" && !title.contains("[Removed]")) {
                    val newsItem = NewsItem(
                        title = title,
                        description = if (description.isEmpty() || description == "null") "No Description" else description,
                        imageUrl = article.optString("urlToImage", ""),
                        publishedAt = formatDate(article.optString("publishedAt", "")),
                        source = article.optJSONObject("source")?.optString("name", "NewsAPI") ?: "NewsAPI",
                        url = article.optString("url", ""),
                        category = determineCategory(title + " " + description),
                        apiSource = "NewsAPI"
                    )
                    allNewsList.add(newsItem)
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to parse news data: ${e.message}")
        }
    }

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
        if (newsList.isNotEmpty()) {
            recyclerView.scrollToPosition(0)
        }
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

    private fun showLoading(show: Boolean) {
        if (show) {
            progressIndicator.show()
        } else {
            progressIndicator.hide()
            swipeRefresh.isRefreshing = false
        }
    }

    private fun showNetworkError() {
        val connectionType = networkManager.getConnectionType()
        val message = when (connectionType) {
            ConnectionType.NONE -> "No internet connection. Please check your network settings."
            else -> "Connection issue. Please try again."
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        swipeRefresh.isRefreshing = false
        showLoading(false)
    }

    private fun handleNetworkError(exception: Exception) {
        val errorMessage = when (exception) {
            is UnknownHostException -> "Cannot connect to server. Please check your internet connection."
            is SocketTimeoutException -> "Connection timeout. Please try again."
            is SSLException -> "Secure connection failed. Please check your network settings."
            else -> "Error loading news: ${exception.message}"
        }

        runOnUiThread {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
    private fun openNewsInBrowser(url: String) {
        if (url.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        networkManager.unregisterNetworkCallback()
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