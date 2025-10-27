package com.example.dbaccess

import com.example.domain.model.Place
import com.example.domain.port.PlaceQueryPort
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedPlaceRepository : PlaceQueryPort {
    override fun findActiveByCity(city: String, limit: Int): List<Place> = transaction {
        PlaceTable
            .select { (PlaceTable.city eq city) and (PlaceTable.defectFlag eq 0.toInt()) }
            .limit(limit)
            .map { it.toPlace() }
    }

    private fun ResultRow.toPlace() = Place(
        id = this[PlaceTable.id],
        name = this[PlaceTable.name],
        city = this[PlaceTable.city],
        shopUrl = this[PlaceTable.shopUrl],
        defectFlag = this[PlaceTable.defectFlag]
    )
}
