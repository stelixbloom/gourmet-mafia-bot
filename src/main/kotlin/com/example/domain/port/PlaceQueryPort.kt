package com.example.domain.port

import com.example.domain.model.Place

interface PlaceQueryPort {
    fun findActiveByCity(city: String, limit: Int = 5): List<Place>
}
