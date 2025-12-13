package com.example.config

object AppConfig {

    val googleApiKey: String = env("GOOGLE_API_KEY")
    val channelSecret: String = env("LINE_CHANNEL_SECRET")
    val channelAccessToken: String = env("LINE_ACCESS_TOKEN")
    val databaseUrl: String = env("DATABASE_URL")  // RenderのPostgres接続URL
    val redisUrl: String = env("REDIS_URL")  // RenderのPostgres接続URL
    val sessionPrefix: String = System.getenv("SESSION_PREFIX") // redisへ格納するセッションのキーの識別子 本番 or 検証
    val monthlyLimit: Int = 300  // ユーザーの月のリクエスト上限数

    private fun env(key: String) =
        System.getenv(key) ?: error("Missing env: $key")
}
