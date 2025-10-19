package com.example.dbaccess

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import java.net.URI

object DatabaseFactory {
    fun init(databaseUrl: String) {
        Database.connect(hikari(databaseUrl))
    }

    private fun hikari(url: String): HikariDataSource {
        // render の DATABASE_URL は postgres://user:pass@host:5432/db
        val uri = URI(url)
        val (user, pass) = (uri.userInfo ?: ":").split(":", limit = 2).let {
            it.getOrNull(0) to it.getOrNull(1)
        }
        val jdbcUrl = "jdbc:postgresql://${uri.host}:${uri.port}${uri.path}?sslmode=require"
        val cfg = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = user
            this.password = pass
            maximumPoolSize = 5
        }
        return HikariDataSource(cfg)
    }
}
