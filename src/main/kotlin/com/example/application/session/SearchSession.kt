package com.example.application.session

import com.example.domain.model.HoursBand
import kotlinx.serialization.Serializable

/**
 * ユーザー入力時のステップ
 */
enum class Step {
    WAIT_AREA, // エリア
    WAIT_GENRE, // ジャンル
    WAIT_SUBGENRE, // サブジャンル
    WAIT_PRICE, // 価格帯
    WAIT_HOURS // 利用シーン（時間帯）
}


/**
 * ユーザー入力保持クラス
 */
@Serializable
data class SearchSession(
    val userId: String,                   // LINEユーザーID
    val step: Step = Step.WAIT_AREA,      // ユーザー入力ステップ
    val area: String? = null,             // エリア
    val genreLabel: String? = null,       // 親ジャンル
    val subgenreLabel: String? = null,    // サブジャンル
    val genreTags: Set<String>? = null,   // 検索用（例: {"和"}）
    val priceLabel: String? = null,       // 表示用
    val priceLevels: Set<Int>? = null,    // 検索用（0..4）
    val hoursLabel: String? = null,       // 表示用
    val hoursBand: HoursBand? = null      // 検索用
)
