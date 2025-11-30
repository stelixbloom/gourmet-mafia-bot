package com.example.application.service

import com.example.domain.model.HoursBand
import com.example.domain.port.PlaceQueryPort
import com.example.interfaceadapters.googleplaces.PlaceCandidate
import com.example.interfaceadapters.googleplaces.PlacesApiClient
import com.example.interfaceadapters.googleplaces.matchesPrice

/**
 *
 */
class SearchService(
    private val placesClient: PlacesApiClient,
    private val repository: PlaceQueryPort
) {
    suspend fun search(
        area: String,
        genreToken: String?,
        priceLevels: Set<Int>?,
        hoursBand: HoursBand?,
        limit: Int = 3
    ): List<SearchResult> {

        val query = placesClient.buildQuery(area = area, genreToken = genreToken, hoursBand = hoursBand)
        val candidates = placesClient.textSearch(query)
        val filtered = candidates.filter { matchesPrice(priceLevels, it.priceLevel) }
        if (filtered.isEmpty()) return emptyList()

        val ids = filtered.map { it.id }
        // DB検索
        val commentsById = repository.findActiveCommentsByIds(ids)

        val (inDb, notInDb) = filtered.partition { commentsById.containsKey(it.id) }

        fun ensureMapsUrl(name: String, id: String, uri: String?): String =
            uri ?: "https://maps.google.com/?q=${name}&cid=${id}"

        // API項目で返す
        fun toResult(c: PlaceCandidate, comment: String?, recommended: Boolean): SearchResult =
            SearchResult(
                id = c.id,
                name = c.name,
                googleMapsUri = ensureMapsUrl(c.name, c.id, c.googleMapsUri),
                priceLevel = c.priceLevel,
                rating = c.rating,
                primaryTypeDisplayName = c.primaryTypeDisplayName,
                comment = comment,
                recommended = recommended
            )

        val prioritized = buildList {
            inDb.forEach { add(toResult(it, commentsById[it.id], true)) } // DBから取得
            if (size < limit) {
                notInDb.forEach { add(toResult(it, null, false)) } // APIから取得
            }
        }

        return prioritized.take(limit)
    }
}
