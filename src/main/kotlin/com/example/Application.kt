package com.example


import com.example.application.usecase.ReplyUseCase
import com.example.config.AppConfig
import com.example.dbaccess.DatabaseFactory
import com.example.dbaccess.ExposedPlaceRepository
import com.example.interfaceadapters.LineApiClient
import com.example.interfaceadapters.LineSignatureVerifier
import com.example.interfaceadapters.LineWebhookController
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

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

    // DB設定
    DatabaseFactory.init(AppConfig.databaseUrl)

    // DI
    val placeRepo = ExposedPlaceRepository()
    val useCase = ReplyUseCase(placeRepo)
    val verifier = LineSignatureVerifier(AppConfig.channelSecret)
    val lineClient = LineApiClient(AppConfig.channelAccessToken)
    val controller = LineWebhookController(verifier, useCase, lineClient)

    routing {
        get("/health") { call.respondText("ok") }
        post("/webhook/line") { controller.post(call) }
    }
}
