package com.example.dbaccess

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import java.net.URI

object DatabaseFactory {

    fun init(databaseUrl: String, schema: String) {
        val dataSource = hikari(databaseUrl, schema)
        Database.connect(dataSource)
    }

    private fun hikari(url: String, schema: String): HikariDataSource {
        val uri = URI(url)
        val (user, pass) = (uri.userInfo ?: ":").split(":", limit = 2)
            .let { it.getOrNull(0) to it.getOrNull(1) }

        // currentSchema でデフォルトスキーマを指定
        val baseJdbcUrl = "jdbc:postgresql://${uri.host}:${uri.port}${uri.path}?sslmode=require"
        val jdbcUrlWithSchema = "$baseJdbcUrl&currentSchema=$schema"

        val cfg = HikariConfig().apply {
            jdbcUrl = jdbcUrlWithSchema
            username = user
            password = pass
            maximumPoolSize = 5

            // 念のため各コネクションに search_path を明示しておく
            connectionInitSql = """SET search_path TO "$schema";"""
        }
        return HikariDataSource(cfg)
    }
}

