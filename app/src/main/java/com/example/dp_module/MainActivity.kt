package com.example.dp_module



import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import kotlin.math.ln
import kotlin.random.Random
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.OkHttpClient



class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val serverUrl = "http://10.0.2.2:5000" // Use this for local server in Android emulator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val websiteInput = findViewById<EditText>(R.id.websiteInput)
        val submitBtn = findViewById<Button>(R.id.submitBtn)
        val resultsBtn = findViewById<Button>(R.id.resultsBtn)
        val outputText = findViewById<TextView>(R.id.outputText)

        submitBtn.setOnClickListener {
            val site = websiteInput.text.toString()
            if (site.isNotEmpty()) {
                val noisySite = addLaplaceNoise(site)
                sendNoisyData(noisySite)
                Toast.makeText(this, "Submitted: $noisySite", Toast.LENGTH_SHORT).show()
            }
        }

        resultsBtn.setOnClickListener {
            fetchResults { result ->
                runOnUiThread {
                    outputText.text = result
                }
            }
        }
    }

    private fun addLaplaceNoise(input: String): String {
        // Convert input to an integer hash
        val hash = input.hashCode()
        val noise = laplaceNoise(1.0)
        val noisedHash = (hash + noise).toInt()
        return noisedHash.toString()
    }

    private fun laplaceNoise(scale: Double): Double {
        val u = Random.nextDouble() - 0.5
        return -scale * kotlin.math.sign(u) * ln(1 - 2 * kotlin.math.abs(u))
    }


    private fun sendNoisyData(noisyWebsite: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val json = JSONObject()
            json.put("website", noisyWebsite)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$serverUrl/submit")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                response.body?.string() // You can log or use this response if needed
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
                val body = response.body
                val resBody = body?.string() ?: ""
                callback(resBody)
            }
        }
    }

}
