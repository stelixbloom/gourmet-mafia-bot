//package com.example.interfaceadapters.googleplaces
//
///**
// * 価格帯/営業時間のクライアント側フィルタ。
// * - priceLevel: ユーザー指定(null=おまかせ)とAPI値を比較
// * - hoursBand: MORNING/LUNCH/DINNER の代表時刻で営業中か判定
// */
//
//private val HOURS_JP = mapOf(
//    HoursBand.MORNING to "モーニング",
//    HoursBand.LUNCH   to "ランチ",
//    HoursBand.DINNER  to "ディナー"
//)
//
//fun buildQuery(area: String, genreToken: String?, hoursBand: HoursBand?): String =
//    listOfNotNull(
//        area.trim().ifBlank { null },
//        genreToken?.trim()?.ifBlank { null },
//        hoursBand?.let { HOURS_JP[it] }
//    ).joinToString(" ")
