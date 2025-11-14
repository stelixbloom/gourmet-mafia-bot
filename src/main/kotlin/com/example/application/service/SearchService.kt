package com.example.application.service

import com.example.domain.model.HoursBand
import com.example.domain.port.PlaceQueryPort
import com.example.interfaceadapters.googleplaces.PlaceCandidate
import com.example.interfaceadapters.googleplaces.PlacesApiClient
import com.example.interfaceadapters.googleplaces.ensureMapsUrl
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
        limit: Int = 5
    ): List<SearchResult> {

        // テキストクエリ作成
        val query = placesClient.buildQuery(area = area, genreToken = genreToken, hoursBand = hoursBand)

        // PlacesAPIテキスト検索
        val candidates = placesClient.textSearch(query)

        // 価格帯フィルタ（priceLevelが無い店も対象）
        val filtered = candidates.filter { matchesPrice(priceLevels, it.priceLevel) }

        if (filtered.isEmpty()) return emptyList()

        // テーブルからで有効IDを取得（comment付き）
        val ids = filtered.map { it.id }
        val commentsById = repository.findActiveCommentsByIds(ids) // Map<id, comment?>

        // DBにある店を優先、無い店も空きがあれば混ぜる
        val (inDb, notInDb) = filtered.partition { commentsById.containsKey(it.id) }

        // API項目で返す（URLはgoogleMapsUri、なければplaceidでURL作成）
        fun toResult(c: PlaceCandidate, comment: String?): SearchResult =
            SearchResult(
                id = c.id,
                name = c.name,
                googleMapsUri = ensureMapsUrl(c.name, c.id, c.googleMapsUri),
                priceLevel = c.priceLevel,
                rating = c.rating,
                primaryTypeDisplayName = c.primaryTypeDisplayName,
                comment = comment
            )

        val prioritized = buildList {
            inDb.forEach { add(toResult(it, commentsById[it.id])) }
            if (size < limit) {
                notInDb.forEach { add(toResult(it, null)) }
            }
        }

        return prioritized.take(limit)
    }
}
