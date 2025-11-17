package com.example.interfaceadapters.googleplaces

import com.example.domain.model.HoursBand
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/**
 * Google Places Text Search (New) クライアント。
 * FieldMask で必要項目だけ取得。
 *
 * 依存: ktor-client-cio, ktor-serialization-kotlinx-json
 */
class PlacesApiClient(private val apiKey: String) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    /**
     * 例: buildQuery("渋谷区", "中華", HoursBand.LUNCH) → "渋谷区 中華 ランチ"
     */
    fun buildQuery(area: String, genreToken: String?, hoursBand: HoursBand?): String =
        listOfNotNull(
            area.trim().ifBlank { null },
            genreToken?.trim()?.ifBlank { null },
            hoursBand?.jpWord
        ).joinToString(" ")

    /**
     * Text Search 実行。
     * 返り値は API の項目をまとめた PlaceCandidate のリスト。
     */
    suspend fun textSearch(
        query: String,
        language: String = "ja",
        region: String = "JP"
    ): List<PlaceCandidate> {
        val resp: TextSearchResponse = client.post("https://places.googleapis.com/v1/places:searchText") {
            contentType(ContentType.Application.Json)
            header("X-Goog-Api-Key", apiKey)
            header(
                // 要件のフィールド
                "X-Goog-FieldMask",
                "places.id,places.displayName,places.priceLevel,places.rating,places.googleMapsUri,places.primaryTypeDisplayName"
            )
            setBody(
                mapOf(
                    "textQuery" to query,
                    "languageCode" to language,
                    "regionCode" to region
                )
            )
        }.body()

        return (resp.places ?: emptyList()).map { it.toCandidate() }
    }
}

/* ===== アプリで使いやすい返却モデル ===== */

data class PlaceCandidate(
    val id: String,                         // "places/ChIJ..."（Place ID）
    val name: String,                       // 表示名
    val priceLevel: Int?,                   // 0..4（null あり）
    val rating: Double?,                    // 1.0..5.0（null あり）
    val googleMapsUri: String?,             // 公式の Maps URL（null なら fallback 生成推奨）
    val primaryTypeDisplayName: String?,    // 例: 「ラーメン店」
    val openingHours: OpeningHoursInfo?     // 営業時間（あれば）
)

/* ===== 営業時間情報 ===== */

data class OpeningHoursInfo(val periods: List<OpeningPeriod>)
data class OpeningPeriod(
    val openDay: Int,   // 0=Sun .. 6=Sat
    val openHour: Int,
    val openMinute: Int,
    val closeDay: Int?, // 24h/未設定なら null
    val closeHour: Int?,
    val closeMinute: Int?
)

/* ====== Google API DTO ====== */

@Serializable
private data class TextSearchResponse(val places: List<GooglePlaceInfo>? = null)

@Serializable
private data class GooglePlaceInfo(
    val id: String,
    val displayName: LocalizedText? = null,
    val priceLevel: PriceLevel? = null,
    val rating: Double? = null,
    val googleMapsUri: String? = null,
    val primaryTypeDisplayName: LocalizedText? = null,
    val regularOpeningHours: OpeningHours? = null
) {
    fun toCandidate() = PlaceCandidate(
        id = id,
        name = displayName?.text ?: "",
        priceLevel = priceLevel?.toInt(),
        rating = rating,
        googleMapsUri = googleMapsUri,
        primaryTypeDisplayName = primaryTypeDisplayName?.text,
        openingHours = regularOpeningHours?.toInfo()
    )
}

@Serializable private data class LocalizedText(val text: String? = null)

@Serializable
private data class OpeningHours(val periods: List<Period>? = null) {
    fun toInfo(): OpeningHoursInfo? {
        val p = periods ?: return null
        return OpeningHoursInfo(
            p.map {
                OpeningPeriod(
                    openDay = it.open?.day ?: return@map null,
                    openHour = it.open.hour ?: 0,
                    openMinute = it.open.minute ?: 0,
                    closeDay = it.close?.day,
                    closeHour = it.close?.hour,
                    closeMinute = it.close?.minute
                )
            }.filterNotNull()
        )
    }
}

@Serializable private data class Period(val open: Point? = null, val close: Point? = null)
@Serializable private data class Point(val day: Int? = null, val hour: Int? = null, val minute: Int? = null)

/** priceLevel enum → 0..4 */
@Serializable
private enum class PriceLevel {
    @SerialName("PRICE_LEVEL_FREE") FREE,
    @SerialName("PRICE_LEVEL_INEXPENSIVE") INEXPENSIVE,
    @SerialName("PRICE_LEVEL_MODERATE") MODERATE,
    @SerialName("PRICE_LEVEL_EXPENSIVE") EXPENSIVE,
    @SerialName("PRICE_LEVEL_VERY_EXPENSIVE") VERY_EXPENSIVE;

    fun toInt(): Int = when (this) {
        FREE -> 0; INEXPENSIVE -> 1; MODERATE -> 2; EXPENSIVE -> 3; VERY_EXPENSIVE -> 4
    }
}

/* ===== クライアント側フィルタ（要件：priceLevel一致 or priceLevelが無い店は通す） ===== */

fun matchesPrice(userLevels: Set<Int>?, apiPriceLevel: Int?): Boolean {
    if (userLevels == null) return true        // おまかせ
    return (apiPriceLevel == null) || userLevels.contains(apiPriceLevel)
}

/* URL フォールバック（googleMapsUri が無い場合に生成） */
fun ensureMapsUrl(name: String, placeId: String, googleMapsUri: String?): String {
    if (!googleMapsUri.isNullOrBlank()) return googleMapsUri
    val q = URLEncoder.encode(name, Charsets.UTF_8)
    val pid = URLEncoder.encode(placeId, Charsets.UTF_8)
    return "https://www.google.com/maps/search/?api=1&query=$q&query_place_id=$pid"
}
