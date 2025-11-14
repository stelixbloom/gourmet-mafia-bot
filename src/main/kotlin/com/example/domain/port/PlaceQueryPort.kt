package com.example.domain.port

import com.example.domain.model.Place
import com.example.domain.model.SearchCriteria

interface PlaceQueryPort {
//    fun findActiveByCity(city: String, limit: Int = 5): List<Place>
//    fun findByCriteria(criteria: SearchCriteria, limit: Int = 5): List<Place>
//    fun findActiveInIds(ids: List<String>, city: String? = null, limit: Int = 5): List<Place>
    /**
     * 指定ID群のうち defect_flag=0 のレコードだけを返す。
     * 値は comment（null可）。存在しなければ Map に入らない。
     */
    fun findActiveCommentsByIds(ids: List<String>): Map<String, String?>
}
