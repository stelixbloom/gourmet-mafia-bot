package com.example

import com.example.di.appModule
import com.example.application.service.SearchService
import com.example.application.session.RedisSessionStore
import com.example.application.session.SessionStore
import com.example.application.usecase.ReplyUseCase
import com.example.config.AppConfig
import com.example.dbaccess.DatabaseFactory
import com.example.dbaccess.PlaceRepository
import com.example.domain.port.PlaceQueryPort
import com.example.interfaceadapters.googleplaces.PlacesApiClient
import com.example.interfaceadapters.line.LineApiClient
import com.example.interfaceadapters.line.LineSignatureVerifier
import com.example.interfaceadapters.line.LineWebhookController
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
import org.koin.core.Koin
import org.koin.ktor.plugin.Koin
import org.koin.ktor.ext.inject
import org.koin.logger.slf4jLogger
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

    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }

//    val logger = LoggerFactory.getLogger("GourmetMafiaApp")

//    // エラーハンドリング用
//    install(StatusPages) {
//        exception<Throwable> { call, cause ->
//            call.respond(
//                status = HttpStatusCode.InternalServerError,
//                message = "Internal Server Error"
//            )
//        }
//    }
//
//    // コンテンツ変換プラグイン
//    install(ContentNegotiation) {
//        json(Json {
//            prettyPrint = true
//            ignoreUnknownKeys = true
//            classDiscriminator = "kind"
//        })
//    }

    // Koin
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    // DB設定
    DatabaseFactory.init(AppConfig.databaseUrl)
//    try {
//        DatabaseFactory.init(AppConfig.databaseUrl)
//        logger.info("Database initialized successfully.")
//    } catch (e: Exception) {
//        logger.error("Failed to initialize database.", e)
//        throw e // 起動中に失敗したら落としてRenderに再起動させる
//    }

//    // DI
//    val places = PlacesApiClient(System.getenv("GOOGLE_MAPS_API_KEY"))
//    val repo: PlaceQueryPort = PlaceRepository()
//    val searchService = SearchService(places, repo)
//    val placeRepo = PlaceRepository()
//    val sessionStore: SessionStore = RedisSessionStore(AppConfig.redisUrl)
//    val useCase = ReplyUseCase(placeRepo, sessionStore)
//    val verifier = LineSignatureVerifier(AppConfig.channelSecret)
//    val lineClient = LineApiClient(AppConfig.channelAccessToken)
//    val controller = LineWebhookController(verifier, useCase, lineClient)

    val controller: LineWebhookController by inject()

    // ルーティング
    routing {
        get("/health") { call.respondText("ok") }
        post("/webhook/line") { controller.post(call) }
    }
//    routing {
//        get("/health") {
//            call.respondText("ok")
//        }
//
//        post("/webhook/line") {
//            logger.info("Incoming webhook request")
//            try {
//                controller.post(call)
//                logger.info("Webhook handled successfully")
//            } catch (e: Exception) {
//                logger.error("Error handling webhook", e)
//                call.respond(HttpStatusCode.InternalServerError)
//            }
//        }
//    }
}
