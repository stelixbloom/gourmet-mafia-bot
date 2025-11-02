package com.example.application.usecase

import com.example.domain.model.HoursBand

object Options {
    // ジャンル
    val GENRE_LABELS = listOf(
        "おまかせ", "和食系", "洋食系", "アジア・エスニック系",
        "肉料理・粉物系", "カレー", "スイーツ・カフェ系", "居酒屋・バー", "麺類"
    )
    fun parseGenre(input: String): Pair<String, Set<String>?>? = when (input) {
        "おまかせ" -> "おまかせ" to null
        "和食系" -> "和食系" to setOf("和食")
        "洋食系" -> "洋食系" to setOf("洋食")
        "アジア・エスニック系" -> "アジア・エスニック系" to setOf("エスニック") // 運用側でマッピング
        "肉料理・粉物系" -> "肉料理・粉物系" to setOf("肉","粉物")
        "カレー" -> "カレー" to setOf("カレー")
        "スイーツ・カフェ系" -> "スイーツ・カフェ系" to setOf("スイーツ","カフェ","パン")
        "居酒屋・バー" -> "居酒屋・バー" to setOf("居酒屋","バー")
        "麺類" -> "麺類" to setOf("麺類")
        else -> null
    }

    // 価格
    val PRICE_LABELS = listOf("おまかせ", "安い", "カジュアル", "やや高め", "お高め")
    fun parsePrice(input: String): Pair<String, Set<Int>?>? = when (input) {
        "おまかせ" -> "おまかせ" to null
        "安い" -> "安い" to setOf(0,1)            // PRICE_LEVEL_FREE, PRICE_LEVEL_INEXPENSIVE
        "カジュアル" -> "カジュアル" to setOf(2)   // PRICE_LEVEL_MODERATE
        "やや高め" -> "やや高め" to setOf(3)      // PRICE_LEVEL_EXPENSIVE
        "お高め" -> "お高め" to setOf(4)          // PRICE_LEVEL_VERY_EXPENSIVE
        else -> null
    }

    // 営業時間
    val HOURS_LABELS = listOf("おまかせ", "モーニング", "ランチ", "ディナー")
    fun parseHours(input: String): Pair<String, HoursBand?>? = when (input) {
        "おまかせ" -> "おまかせ" to null
        "モーニング" -> "モーニング" to HoursBand.MORNING
        "ランチ" -> "ランチ" to HoursBand.LUNCH
        "ディナー" -> "ディナー" to HoursBand.DINNER
        else -> null
    }
}