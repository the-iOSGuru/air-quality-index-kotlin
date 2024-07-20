package com.example.aqiandroid// NetworkManager.kt
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

// Error Enum
enum class NetworkError {
    INVALID_URL,
    REQUEST_FAILED,
    DECODING_FAILED,
    OVER_QUOTA,
    INVALID_KEY,
    UNKNOWN_ERROR,
    INVALID_DATA
}

class NetworkManager(private val context: Context) {

    private val client = OkHttpClient()
    private val gson = Gson()

    private fun buildUrl(lat: Double, lng: Double, token: String): HttpUrl? {
        val baseUrl = context.getString(R.string.api_base_url) // e.g., "api.waqi.info"
        return try {
            HttpUrl.Builder()
                .scheme("https")
                .host(baseUrl) // Use the base URL as the host
                .addPathSegment("feed")
                .addPathSegment("geo:$lat;$lng") // Include colon in the path segment
                .addQueryParameter("token", token)
                .build()
        } catch (e: IllegalArgumentException) {
            // Handle case where baseUrl is not a valid URL
            null
        }
    }

    suspend fun fetchAirQualityData(lat: Double, lng: Double, token: String): GeoData? {
        val url = buildUrl(lat, lng, token) ?: throw NetworkException(NetworkError.INVALID_URL)

        val request = Request.Builder()
            .url(url)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response: Response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw NetworkException(NetworkError.REQUEST_FAILED)
                }

                val responseData = response.body?.string() ?: throw NetworkException(NetworkError.INVALID_DATA)

                val decodedData = gson.fromJson(responseData, GeoData::class.java)

                if (decodedData.status == "error") {
                    throw NetworkException(mapError(responseData))
                }

                decodedData
            } catch (e: JsonSyntaxException) {
                throw NetworkException(NetworkError.DECODING_FAILED)
            } catch (e: Exception) {
                throw NetworkException(NetworkError.UNKNOWN_ERROR)
            }
        }
    }

    private fun mapError(data: String): NetworkError {
        return try {
            val errorResponse = gson.fromJson(data, ErrorResponse::class.java)
            when (errorResponse.message) {
                "Over quota" -> NetworkError.OVER_QUOTA
                "Invalid key" -> NetworkError.INVALID_KEY
                else -> NetworkError.UNKNOWN_ERROR
            }
        } catch (e: JsonSyntaxException) {
            NetworkError.DECODING_FAILED
        }
    }
}

class NetworkException(val error: NetworkError) : Exception() {
    override val message: String
        get() = error.toString() // Customize this to provide a meaningful message
}