package com.example.config

object AppConfig {
    val channelSecret: String = env("LINE_CHANNEL_SECRET")
    val channelAccessToken: String = env("LINE_ACCESS_TOKEN")
    val databaseUrl: String = env("DATABASE_URL")  // RenderのPostgres接続URL
    val redisUrl: String = env("REDIS_URL")  // RenderのPostgres接続URL

    private fun env(key: String) =
        System.getenv(key) ?: error("Missing env: $key")
}
