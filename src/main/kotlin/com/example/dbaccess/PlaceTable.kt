package com.example.dbaccess

import org.jetbrains.exposed.sql.Table

object PlaceTable : Table("tbl_places") {
    val id = text("id")
    val name = text("name").nullable()
    val areaInfo = text("area_info").nullable()
    val rating = text("rating").nullable()
    val comment = text("comment").nullable()
    val defectFlag = short("defect_flag").default(0)
    override val primaryKey = PrimaryKey(id)
}