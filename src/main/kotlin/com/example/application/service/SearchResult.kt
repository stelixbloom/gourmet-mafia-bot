package com.example.application.service

data class SearchResult(
    val id: String,
    val name: String,
    val googleMapsUri: String,
    val priceLevel: Int?,
    val rating: Double?,
    val primaryTypeDisplayName: String?,
    val comment: String?,
    val recommended: Boolean = false
)
