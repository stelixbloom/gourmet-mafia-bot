package com.example.dbaccess

import org.jetbrains.exposed.sql.Table

object PlaceTable : Table("tbl_places") {
    val id = text("id")                 // place_id
    val name = text("name")
    val city = text("city")
    val shopUrl = text("shop_url")
    val defectFlag = bool("defect_flag")
    override val primaryKey = PrimaryKey(id)
}
