package com.example.aqiandroid

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.aqiandroid.ui.theme.AQIAndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    // Dependency properties
    private lateinit var networkManager: NetworkManager
    private lateinit var locationManager: LocationManager
    private var token: String? = null

    // State variables
    private var o3ForecastData by mutableStateOf(listOf<O3>())
    private var pm10ForecastData by mutableStateOf(listOf<O3>())
    private var pm25ForecastData by mutableStateOf(listOf<O3>())
    private var filteredData by mutableStateOf(listOf<O3>())
    private var isLoading by mutableStateOf(false)
    private var latText by mutableStateOf("")
    private var longText by mutableStateOf("")
    private var cityStateText by mutableStateOf("")
    private var stationText by mutableStateOf("")
    private var aqiText by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AQIAndroidTheme {
                MainScreen()
            }
        }

        // Initialize dependencies
        networkManager = NetworkManager(this)
        locationManager = LocationManager(this)
        token = getString(R.string.aqi_token)

        // Check if the token is available
        if (token!!.isEmpty()) {
            showToast("API token is missing. Please check your configuration.")
        }
        requestLocationUpdates()
    }

    @Composable
    fun CurrentAQIText(aqi: String) {
        // Convert AQI value to an integer and make the text colored based on the value
        val aqiValue = aqi.toIntOrNull() ?: 0
        val coloredAQIText = makeColoredAQIText(aqiValue)

        BasicText(
            text = coloredAQIText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(8.dp)
        )
    }

    @Composable
    fun ColoredInstructionsText() {
        // Create a text with colored instructions for AQI levels
        val instructions = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Green, fontSize = 16.sp)) {
                append("Green - Good")
            }
            append(", ")
            withStyle(style = SpanStyle(color = Color.Yellow, fontSize = 16.sp)) {
                append("Yellow - Moderate")
            }
            append(", ")
            withStyle(style = SpanStyle(color = Color(0xFFFFA500), fontSize = 16.sp)) { // Orange color
                append("Orange - Unhealthy for Sensitive Groups")
            }
            append(", ")
            withStyle(style = SpanStyle(color = Color.Red, fontSize = 16.sp)) {
                append("Red - Unsafe")
            }
        }

        BasicText(
            text = instructions,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    @Composable
    fun MainScreen() {
        val backgroundColor = colorResource(id = R.color.black_background)
        val textColor = colorResource(id = R.color.text_white)
        val cardBackgroundColor = colorResource(id = R.color.highlight_green)

        var selectedDataType by remember { mutableIntStateOf(0) }

        // Determine which data to show based on selectedDataType
        val dataToShow = when (selectedDataType) {
            1 -> pm10ForecastData
            2 -> pm25ForecastData
            else -> o3ForecastData
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .background(color = backgroundColor)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Display current location and AQI
            Text("Current Location: $cityStateText", color = textColor)
            Text("Station name: $stationText", color = textColor)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Current AQI: ", color = textColor)
                CurrentAQIText(aqiText)
            }

            ColoredInstructionsText()

            Spacer(modifier = Modifier.height(16.dp))

            // Latitude input field
            LabeledTextField(
                label = "Latitude:",
                value = latText,
                onValueChange = { newLat -> latText = newLat },
                textColor = textColor,
                backgroundColor = cardBackgroundColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Longitude input field
            LabeledTextField(
                label = "Longitude:",
                value = longText,
                onValueChange = { newLng -> longText = newLng },
                textColor = textColor,
                backgroundColor = cardBackgroundColor
            )

            // Buttons for updating and resetting location
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    val lat = latText.toDoubleOrNull() ?: 0.0
                    val lng = longText.toDoubleOrNull() ?: 0.0
                    fetchData(lat, lng)
                }) {
                    Text("Update")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    requestLocationUpdates()
                }) {
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector for data types
            DataSelector(
                textColor = textColor,
                selectedOption = selectedDataType,
                onOptionSelected = { index -> selectedDataType = index }
            )

            // Display the forecast data
            ForecastList(dataToShow)
        }
    }

    @Composable
    fun LabeledTextField(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        textColor: Color,
        backgroundColor: Color
    ) {
        // Text field with a label
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                modifier = Modifier
                    .width(100.dp) // Adjust width as needed
                    .padding(end = 8.dp),
                style = MaterialTheme.typography.labelMedium.copy(color = textColor) // Set label text color
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = textColor),
                modifier = Modifier
                    .weight(1f)
                    .background(color = backgroundColor)
                    .border(1.dp, Color.Gray)
                    .padding(8.dp)
            )
        }
    }

    @Composable
    fun DataSelector(
        selectedOption: Int,
        onOptionSelected: (Int) -> Unit,
        textColor: Color
    ) {
        // Selector for choosing between o3, pm10, and pm25 data
        val options = listOf("o3", "pm10", "pm25")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, label ->
                Row(
                    modifier = Modifier
                        .clickable { onOptionSelected(index) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == index,
                        onClick = { onOptionSelected(index) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }
        }
    }

    @Composable
    fun ForecastList(data: List<O3>) {
        // Display a horizontal list of forecast items
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            items(data) { forecast ->
                ForecastItem(forecast)
            }
        }
    }

    @Composable
    fun ForecastItem(forecast: O3) {
        val greenBackgroundColor = colorResource(id = R.color.highlight_green)
        val whiteText = colorResource(id = R.color.text_white)

        Card(
            modifier = Modifier
                .padding(8.dp)
                .width(200.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color = greenBackgroundColor),
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .background(color = greenBackgroundColor),
            ) {
                Text(
                    text = forecast.dayOfWeek ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(color = whiteText),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = forecast.day,
                    style = MaterialTheme.typography.bodyMedium.copy(color = whiteText),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Avg AQI: ${forecast.avg}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = whiteText),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Max AQI: ${forecast.max}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = whiteText),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Min AQI: ${forecast.min}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = whiteText),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    private fun makeColoredAQIText(aqi: Int): AnnotatedString {
        // Generate a colored text based on AQI value
        val color = when (aqi) {
            in 0..50 -> Color.Green
            in 51..100 -> Color.Yellow
            in 101..150 -> Color(0xFFFFA500) // Orange
            in 151..Int.MAX_VALUE -> Color.Red
            else -> Color.Black
        }

        return buildAnnotatedString {
            withStyle(style = SpanStyle(color = color)) {
                append("$aqi")
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun fetchData(lat: Double, lng: Double) {
        // Fetch air quality data based on latitude and longitude
        val formattedLat = String.format("%.2f", lat).toDouble()
        val formattedLng = String.format("%.2f", lng).toDouble()

        isLoading = true
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    networkManager.fetchAirQualityData(formattedLat, formattedLng, token ?: "")
                }
                response?.let { updateUI(it) }
            } catch (e: Exception) {
                showToast("An error occurred: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun updateUI(geoData: GeoData) {
        // Update UI components with the fetched data
        cityStateText = "${geoData.data.city.name} ${geoData.data.city.location}"
        stationText = geoData.data.attributions.firstOrNull()?.name ?: ""
        aqiText = "${geoData.data.aqi}"
        o3ForecastData = filterForecastData(geoData.data.forecast.daily.o3)
        pm10ForecastData = filterForecastData(geoData.data.forecast.daily.pm10)
        pm25ForecastData = filterForecastData(geoData.data.forecast.daily.pm25)
        filteredData = o3ForecastData
    }

    private fun filterForecastData(forecastData: List<O3>): List<O3> {
        // Filter and format forecast data based on current date and time range
        val calendar = Calendar.getInstance()
        val oneDayBefore = calendar.apply { add(Calendar.DAY_OF_YEAR, -2) }.time
        calendar.apply { add(Calendar.DAY_OF_YEAR, 3) }
        val oneDayAfter = calendar.time

        // Define date formatters
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val dayOfWeekFormatter = SimpleDateFormat("EEEE", Locale.getDefault())

        return forecastData.mapNotNull { forecast ->
            val forecastDate = dateFormatter.parse(forecast.day) ?: return@mapNotNull null
            if (forecastDate >= oneDayBefore && forecastDate <= oneDayAfter) {
                forecast.day = displayDateFormatter.format(forecastDate)
                forecast.dayOfWeek = dayOfWeekFormatter.format(forecastDate)
                forecast
            } else {
                null
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun requestLocationUpdates() {
        // Request location updates if permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        locationManager.requestLocation { location ->
            location?.let {
                latText = String.format("%.2f", it.latitude)
                longText = String.format("%.2f", it.longitude)
                fetchData(it.latitude, it.longitude)
            }
        }
    }

    private fun showToast(message: String) {
        // Show a toast message to the user
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}