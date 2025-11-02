package com.example.application.session

import com.example.domain.model.HoursBand
import kotlinx.serialization.Serializable

enum class Step {
    WAIT_AREA, // エリア
    WAIT_GENRE, // ジャンル
    WAIT_PRICE, // 価格帯
    WAIT_HOURS // 利用シーン（営業時間）
}

@Serializable
data class SearchSession(
    val userId: String,
    val step: Step = Step.WAIT_AREA,
    val city: String? = null,
    val genreLabel: String? = null,       // 表示用（例: "和食系"）
    val genreTags: Set<String>? = null,   // 検索用（例: {"和"}）
    val priceLabel: String? = null,       // 表示用
    val priceLevels: Set<Int>? = null,    // 検索用（0..4）
    val hoursLabel: String? = null,       // 表示用
    val hoursBand: HoursBand? = null      // 検索用
)
