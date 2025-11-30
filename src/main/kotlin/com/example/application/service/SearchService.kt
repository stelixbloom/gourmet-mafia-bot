package com.example.application.service

import com.example.domain.model.HoursBand
import com.example.domain.port.PlaceQueryPort
import com.example.interfaceadapters.googleplaces.PlaceCandidate
import com.example.interfaceadapters.googleplaces.GooglePlacesApiClient
import com.example.interfaceadapters.googleplaces.matchesPrice
import com.example.interfaceadapters.googleplaces.matchesHoursBand

/**
 *
 */
class SearchService(
    private val googleClient: GooglePlacesApiClient,
    private val repository: PlaceQueryPort
) {
    suspend fun search(
        area: String,
        genreToken: String?,
        priceLevels: Set<Int>?,
        hoursBand: HoursBand?,
        limit: Int = 3
    ): List<SearchResult> {

        val query = googleClient.buildQuery(area = area, genreToken = genreToken, hoursBand = hoursBand)
        val candidates = googleClient.textSearch(query)

        val now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Tokyo"))

        val filtered = candidates
            .filter { matchesPrice(priceLevels, it.priceLevel) }
            .filter { matchesHoursBand(hoursBand, it.openingHours, now) }

        if (filtered.isEmpty()) return emptyList()

        val ids = filtered.map { it.id }
        val commentsById = repository.findActiveCommentsByIds(ids)

        val (inDb, notInDb) = filtered.partition { commentsById.containsKey(it.id) }

        fun ensureMapsUrl(name: String, id: String, uri: String?): String =
            uri ?: "https://maps.google.com/?q=${name}&cid=${id}"

        fun toResult(response: PlaceCandidate, comment: String?, recommended: Boolean): SearchResult =
            SearchResult(
                id = response.id,
                name = response.name,
                googleMapsUri = ensureMapsUrl(response.name, response.id, response.googleMapsUri),
                priceLevel = response.priceLevel,
                rating = response.rating,
                primaryTypeDisplayName = response.primaryTypeDisplayName,
                comment = comment,
                recommended = recommended
            )

        val prioritized = buildList {
            inDb.forEach { add(toResult(it, commentsById[it.id], true)) }
            if (size < limit) {
                notInDb.forEach { add(toResult(it, null, false)) }
            }
        }

        return prioritized.take(limit)
    }
}
