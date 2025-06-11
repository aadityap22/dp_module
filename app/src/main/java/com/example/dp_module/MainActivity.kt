package com.example.dp_module

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import kotlin.random.Random
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val serverUrl = "http://10.0.2.2:5000" // Emulator -> localhost

    // Predefined website list
    private val websiteList = listOf(
        "google.com", "youtube.com", "facebook.com", "twitter.com", "instagram.com",
        "stackoverflow.com", "linkedin.com", "netflix.com", "github.com", "reddit.com",
        "microsoft.com"
    )

    private val p = 0.75  // Probability to report true index

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val websiteInput = findViewById<EditText>(R.id.websiteInput)
        val submitBtn = findViewById<Button>(R.id.submitBtn)
        val resultsBtn = findViewById<Button>(R.id.resultsBtn)
        val outputText = findViewById<TextView>(R.id.outputText)

        submitBtn.setOnClickListener {
            val site = websiteInput.text.toString().trim()
            val index = websiteList.indexOf(site)

            if (index != -1) {
                sendNoisyDataRR(index)
                Toast.makeText(this, "Submitted with randomized response", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Invalid website. Choose from list.", Toast.LENGTH_LONG).show()
            }
        }

        resultsBtn.setOnClickListener {
            fetchResults { result ->
                runOnUiThread {
                    val counts = parseServerResults(result)
                    val totalReports = counts.sum()
                    val decodedCounts = decodeCountsRR(counts, totalReports)

                    val display = websiteList.indices.joinToString("\n") { i ->
                        "${websiteList[i]}: ${"%.1f".format(decodedCounts[i])}"
                    }
                    outputText.text = display
                }
            }
        }
    }

    // Randomized Response submission
    private fun sendNoisyDataRR(trueIndex: Int) {
        val n = websiteList.size
        val reportIndex = if (Random.nextDouble() < p) {
            trueIndex
        } else {
            var randIndex: Int
            do {
                randIndex = Random.nextInt(n)
            } while (randIndex == trueIndex)
            randIndex
        }

        CoroutineScope(Dispatchers.IO).launch {
            val json = JSONObject()
            json.put("index", reportIndex)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$serverUrl/submit")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                response.body?.string() // Optional: log or handle response
            }
        }
    }

    private fun fetchResults(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val request = Request.Builder()
                .url("$serverUrl/results")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val resBody = response.body?.string() ?: ""
                callback(resBody)
            }
        }
    }

    private fun parseServerResults(jsonStr: String): List<Int> {
        val json = JSONObject(jsonStr)
        val counts = mutableListOf<Int>()

        for (i in websiteList.indices) {
            counts.add(json.optInt(i.toString(), 0))
        }
        return counts
    }

    private fun decodeCountsRR(counts: List<Int>, totalReports: Int): List<Double> {
        val n = websiteList.size
        val decoded = mutableListOf<Double>()

        for (j in 0 until n) {
            val cj = counts[j].toDouble()
            val est = (cj - ((1 - p) / n) * totalReports) / (p - (1 - p) / n)
            decoded.add(if (est < 0) 0.0 else est)
        }

        return decoded
    }
}
