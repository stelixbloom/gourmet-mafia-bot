package com.example.config

object AppConfig {

    val googleApiKey: String = env("GOOGLE_API_KEY")
    val channelSecret: String = env("LINE_CHANNEL_SECRET")
    val channelAccessToken: String = env("LINE_ACCESS_TOKEN")
    val databaseUrl: String = env("DATABASE_URL")  // RenderのPostgres接続URL
    val redisUrl: String = env("REDIS_URL")  // RenderのPostgres接続URL
    val monthlyLimit: Int = 300  // RenderのPostgres接続URL

    private fun env(key: String) =
        System.getenv(key) ?: error("Missing env: $key")
}
