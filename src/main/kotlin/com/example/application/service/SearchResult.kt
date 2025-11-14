package com.example.application.service

data class SearchResult(
    val id: String,                       // Place ID ("places/ChIJ...")
    val name: String,
    val googleMapsUri: String,
    val priceLevel: Int?,                 // 0..4
    val rating: Double?,                  // 1..5
    val primaryTypeDisplayName: String?,  // 例: 「ラーメン店」
    val comment: String?                  // DBにあるときだけ付く
)
