package com.example.weatherapp

import android.app.AlertDialog
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.weatherapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import kotlin.math.roundToInt
import kotlinx.coroutines.MainScope
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*

private lateinit var binding: ActivityMainBinding
private lateinit var response: String

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            CoroutineScope(IO).launch {
                try {
                    callAPI()
                }catch (e: java.lang.Exception){
                    showTextPopup("Error make sure you entered a Zip code and are connected to the internet.")
                }
            }
        }

    }

    private suspend fun callAPI(){
        val result = getResulFromAPI()
        populateInfo(result)
    }

    private fun getResulFromAPI(): String{
        try {
            val zip = binding.editTextNumberSigned.text

            val myURL = "https://api.openweathermap.org/data/2.5/weather?zip=$zip,us&units=imperial&appid=507c7d46a66adf806d89048fd606bd04"
            response = URL(myURL).readText(
                Charsets.UTF_8
            )
        }catch (e: java.lang.Exception){
            response = "ERR: $e"
        }
        return response
    }

    private suspend fun populateInfo(incoming: String){
        withContext(Main){
            val jsonObj = JSONObject(incoming)
            val main = jsonObj.getJSONObject("main")
            val temp = main.getString("temp").toFloat().roundToInt()
            val max = main.getString("temp_max").toFloat().roundToInt()
            val min = main.getString("temp_min").toFloat().roundToInt()

            val wind = jsonObj.getJSONObject("wind")
            val speed = wind.getString("speed").toFloat().roundToInt()
            val gust = wind.getString("gust").toFloat().roundToInt()

            val sun = jsonObj.getJSONObject("sys")
            val up = convertTimeStamp(binding.editTextNumberSigned.text.toString().toInt(), sun.getString("sunrise").toInt())
            val down = convertTimeStamp(binding.editTextNumberSigned.text.toString().toInt(), sun.getString("sunset").toInt())

            binding.temp.text = "$temp°F"
            binding.max.text = "Max: $max°F"
            binding.min.text = "Min: $min°F"
            binding.wind.text = "${speed}mph"
            binding.gust.text = "gust: $gust"
            binding.sunrise.text = "sunrise: $up"
            binding.sunset.text = "sunset: $down"

        }
    }

    private fun showTextPopup(message: String) {
        val context: Context = this
        launch {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun getTimeZoneIdFromZipCode(zipCode: Int): String {
        val url = URL("http://api.timezonedb.com/v2.1/get-time-zone?key=\tLXOQU2WLXHL1&format=json&by=postal-code&postalCode=$zipCode&country=US")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
            it.readText()
        }

        val responseJson = JSONObject(response)
        return responseJson.getString("zoneName")
    }
    private fun convertTimeStamp(zip: Int, unix: Int): String {
        // Get the time zone for the given zip code
        val tz = TimeZone.getTimeZone(TimeZone.getAvailableIDs().firstOrNull { it.endsWith(zip.toString()) } ?: "UTC")

        // Convert the Unix timestamp to a date and time
        val date = Date(unix * 1000L)

        // Format the date and time
        val formatter = SimpleDateFormat("HH:mm:ss")
        formatter.timeZone = tz
        val dateString = formatter.format(date)

        return dateString
    }
}