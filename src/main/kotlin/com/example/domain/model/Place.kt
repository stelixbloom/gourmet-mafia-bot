package com.example.domain.model

data class Place(
    val id: String,          // GooglePlaceID（の予定）
    val name: String,        // 店名
    val city: String,        // 東京都渋谷区 等の 都道府県市区町村
    val shopUrl: String,     // GoogleMapsURL
    val active: Boolean      // 有効/無効 フラグ
)
