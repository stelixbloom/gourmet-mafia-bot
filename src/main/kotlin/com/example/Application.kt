package com.example


import com.example.application.usecase.ReplyUseCase
import com.example.config.AppConfig
import com.example.dbaccess.DatabaseFactory
import com.example.dbaccess.ExposedPlaceRepository
import com.example.interfaceadapters.LineApiClient
import com.example.interfaceadapters.LineSignatureVerifier
import com.example.interfaceadapters.LineWebhookController
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.statuspages.StatusPages
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

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

    val logger = LoggerFactory.getLogger("GourmetMafiaApp")

    // エラーハンドリング用
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = "Internal Server Error"
            )
        }
    }

    // コンテンツ変換プラグイン
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            classDiscriminator = "kind"
        })
    }

    // DB設定
    try {
        DatabaseFactory.init(AppConfig.databaseUrl)
        logger.info("Database initialized successfully.")
    } catch (e: Exception) {
        logger.error("Failed to initialize database.", e)
        throw e // 起動中に失敗したら落としてRenderに再起動させる
    }

    // DI
    val placeRepo = ExposedPlaceRepository()
    val useCase = ReplyUseCase(placeRepo)
    val verifier = LineSignatureVerifier(AppConfig.channelSecret)
    val lineClient = LineApiClient(AppConfig.channelAccessToken)
    val controller = LineWebhookController(verifier, useCase, lineClient)

    // ルーティング
    routing {
        get("/health") {
            logger.info("🩺 Health check OK")
            call.respondText("ok")
        }

        post("/webhook/line") {
            logger.info("Incoming webhook request")
            try {
                controller.post(call)
                logger.info("Webhook handled successfully")
            } catch (e: Exception) {
                logger.error("Error handling webhook", e)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
