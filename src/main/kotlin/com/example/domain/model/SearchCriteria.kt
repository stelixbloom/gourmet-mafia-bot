package com.example.domain.model

data class SearchCriteria(
    val city: String,
    val genreTags: Set<String>? = null,     // 例: {"和","麺類"} など。nullなら指定なし
    val priceLevels: Set<Int>? = null,      // Google price_level 0..4（0=FREE, 4=VERY_EXPENSIVE）
    val hoursBand: HoursBand? = null        // モーニング/ランチ/ディナー/おまかせ
)

enum class HoursBand { MORNING, LUNCH, DINNER }