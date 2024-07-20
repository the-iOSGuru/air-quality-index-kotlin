package com.example.aqiandroid
data class GeoData (
    val status: String,
    val data: Data
)

data class Data (
    val aqi: Long,
    val idx: Long,
    val attributions: List<Attribution>,
    val city: City,
    val dominentpol: String,
    val iaqi: Iaqi,
    val time: Time,
    val forecast: Forecast,
    val debug: Debug
)

data class Attribution (
    val url: String,
    val name: String,
    val logo: String? = null
)

data class City (
    val geo: List<Double>,
    val name: String,
    val url: String,
    val location: String
)

data class Debug (
    val sync: String
)

data class Forecast (
    val daily: Daily
)

data class Daily (
    val o3: List<O3>,
    val pm10: List<O3>,
    val pm25: List<O3>
)

data class O3 (
    val avg: Long,
    var day: String,
    var dayOfWeek: String? = null,
    val max: Long,
    val min: Long
)

data class Iaqi (
    val h: H,
    val p: H,
    val pm25: H,
    val t: H,
    val w: H,
    val wg: H
)

data class H (
    val v: Double
)

data class Time (
    val s: String,
    val tz: String,
    val v: Long,
    val iso: String
)
