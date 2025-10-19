package com.example.dbaccess

import org.jetbrains.exposed.sql.Table

object PlaceTable : Table("places") {
    val id = text("id")                 // place_id
    val name = text("name")
    val city = text("city")
    val shopUrl = text("shop_url")
    val active = bool("active_flag").default(true)
    override val primaryKey = PrimaryKey(id)
}
