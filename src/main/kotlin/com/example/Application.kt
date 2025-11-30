package com.example

import com.example.di.appModule
import com.example.config.AppConfig
import com.example.dbaccess.DatabaseFactory
import com.example.interfaceadapters.line.LineWebhookController
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin
import org.koin.ktor.ext.inject
import org.koin.logger.slf4jLogger

/**
 * Main
 * アプリ起動時の設定あり
 */
fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }

    // Koin
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    // DB設定
    DatabaseFactory.init(AppConfig.databaseUrl)

    val controller: LineWebhookController by inject()

    // ルーティング
    routing {
        get("/health") { call.respondText("ok") }
        post("/webhook/line") { controller.post(call) }
    }
}
