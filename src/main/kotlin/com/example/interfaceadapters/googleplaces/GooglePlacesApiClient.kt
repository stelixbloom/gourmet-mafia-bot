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
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Google Places Text Search (New) クライアント。
 * FieldMask で必要な項目だけ取得。
 */
class GooglePlacesApiClient(private val apiKey: String) {

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
            hoursBand?.hoursSearchWord
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
                "X-Goog-FieldMask",
                "places.id,places.displayName,places.priceLevel,places.rating,places.googleMapsUri,places.primaryTypeDisplayName,places.regularOpeningHours.periods,places.regularOpeningHours.weekdayDescriptions"
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

/**
 * GooglePlacesAPIのレスポンスクラス
 */
data class PlaceCandidate(
    val id: String,                         // "places/ChIJ..."（Place ID）
    val name: String,                       // 表示名
    val priceLevel: Int?,                   // 0..4（null あり）
    val rating: Double?,                    // 1.0..5.0（null あり）
    val googleMapsUri: String?,             // 公式の Maps URL（null なら fallback 生成推奨）
    val primaryTypeDisplayName: String?,    // 例: 「ラーメン店」
    val openingHours: OpeningHoursInfo?     // 営業時間（あれば）
)

/**
 * 営業時間情報
 */
data class OpeningHoursInfo(val periods: List<OpeningPeriod>)

data class OpeningPeriod(
    val openDay: Int,   // 0=Sun .. 6=Sat
    val openHour: Int,
    val openMinute: Int,
    val closeDay: Int?, // 24h/未設定なら null
    val closeHour: Int?,
    val closeMinute: Int?
)

/**
 * Google API DTO
 */
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

/**
 * クライアント側フィルタ（要件：priceLevel一致 or priceLevelが無い店は通す）
 */
fun matchesPrice(userLevels: Set<Int>?, apiPriceLevel: Int?): Boolean {
    if (userLevels == null) return true        // おまかせ
    return (apiPriceLevel == null) || userLevels.contains(apiPriceLevel)
}


private const val MINUTES_PER_DAY = 24 * 60
private const val MINUTES_PER_WEEK = 7 * MINUTES_PER_DAY

/**
 * Googleの day(0=Sun..6=Sat) + 時刻 から
 * "週の先頭からの分数" に変換
 */
private fun toWeekMinutes(day: Int, hour: Int, minute: Int): Int {
    return day * MINUTES_PER_DAY + hour * 60 + minute
}

/**
 * OpeningHoursInfo が、指定の曜日・時刻で営業中かどうか
 */
fun OpeningHoursInfo.isOpenAt(dayOfWeek: DayOfWeek, time: LocalTime): Boolean {
    // DayOfWeek.MONDAY.value = 1 ... SUNDAY = 7
    // Google: 0=Sun..6=Sat なので %7 で合わせる
    val googleDay = dayOfWeek.value % 7
    val target = toWeekMinutes(googleDay, time.hour, time.minute)

    return periods.any { p ->
        val openDay = p.openDay
        val closeDay = p.closeDay ?: openDay
        val open = toWeekMinutes(openDay, p.openHour, p.openMinute)
        val close = if (p.closeHour != null && p.closeMinute != null) {
            toWeekMinutes(closeDay, p.closeHour, p.closeMinute)
        } else {
            // close が無い店はとりあえず24時間営業扱いにするならこれでもいいし、
            // 除外したいなら false を返す実装に変えてもいい
            toWeekMinutes(closeDay, 23, 59)
        }

        var start = open
        var end = close

        // 日またぎ（open > close）の場合は週末をまたいで補正
        if (end <= start) {
            end += MINUTES_PER_WEEK
        }

        // target も週またぎ考慮して2パターンで判定
        val t1 = target
        val t2 = target + MINUTES_PER_WEEK

        (t1 in start until end) || (t2 in start until end)
    }
}

/**
 * hoursBand の条件を満たすかどうか
 * - hoursBand == null → フィルタしない（true）
 * - openingHours == null → 今は通す方針（priceLevelと同じノリ）
 */
fun matchesHoursBand(
    hoursBand: HoursBand?,
    openingHours: OpeningHoursInfo?,
    now: ZonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
): Boolean {
    if (hoursBand == null) return true
    if (openingHours == null) return true  // 厳しくしたければ false に変える

    val day = now.dayOfWeek

    val targetTimes: List<LocalTime> = when (hoursBand) {
        HoursBand.MORNING -> listOf(LocalTime.of(10, 0))
        HoursBand.LUNCH   -> listOf(LocalTime.of(12, 0))
        HoursBand.DINNER  -> listOf(
            LocalTime.of(19, 0),
            LocalTime.of(23, 0)
        )
    }

    return targetTimes.any { t -> openingHours.isOpenAt(day, t) }
}

