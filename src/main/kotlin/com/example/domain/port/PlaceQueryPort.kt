package com.example.domain.port

import com.example.domain.model.Place
import com.example.domain.model.SearchCriteria

interface PlaceQueryPort {
//    fun findActiveByCity(city: String, limit: Int = 5): List<Place>
    fun findByCriteria(criteria: SearchCriteria, limit: Int = 5): List<Place>
}
