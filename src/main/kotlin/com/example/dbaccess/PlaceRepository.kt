package com.example.dbaccess

import com.example.domain.port.PlaceQueryPort
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class PlaceRepository : PlaceQueryPort {

    override fun findActiveCommentsByIds(ids: List<String>): Map<String, String?> = transaction {
        if (ids.isEmpty()) return@transaction emptyMap()
        PlaceTable
            .slice(PlaceTable.id, PlaceTable.comment)
            .select { (PlaceTable.id inList ids) and (PlaceTable.defectFlag eq 0.toShort()) }
            .associate { it[PlaceTable.id] to it[PlaceTable.comment] }
    }
}
