package com.example.dbaccess

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

object DatabaseFactory {

    fun init(databaseUrl: String, schema: String) {
        // DB 接続
        val db = Database.connect(hikari(databaseUrl))

        // 接続直後に search_path をスキーマに切り替える
        transaction(db) {
            exec("""SET search_path TO "$schema";""")
        }
    }

    private fun hikari(url: String): HikariDataSource {
        val uri = URI(url)
        val (user, pass) = (uri.userInfo ?: ":").split(":", limit = 2)
            .let { it.getOrNull(0) to it.getOrNull(1) }

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
