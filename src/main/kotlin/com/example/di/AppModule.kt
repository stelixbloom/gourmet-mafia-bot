package com.example.di

import com.example.application.service.MonthlyQuotaService
import com.example.application.service.RedisMonthlyQuotaService
import com.example.application.service.SearchService
import com.example.application.usecase.ReplyUseCase
import com.example.config.AppConfig
import com.example.domain.port.PlaceQueryPort
import com.example.dbaccess.PlaceRepository
import com.example.interfaceadapters.googleplaces.GooglePlacesApiClient
import com.example.application.session.RedisSessionStore
import com.example.application.session.SessionStore
import com.example.interfaceadapters.line.LineApiClient
import com.example.interfaceadapters.line.LineSignatureVerifier
import com.example.interfaceadapters.line.LineWebhookController
import org.koin.dsl.module
import redis.clients.jedis.JedisPooled

val appModule = module {

    // Redisクライアント（共通）
    single {
        JedisPooled(AppConfig.redisUrl)
    }

    // Session
    single<SessionStore> {
        RedisSessionStore(
            client = get(),  // JedisPooled
            prefix = AppConfig.sessionPrefix  // "prod" or "dev"
        )  // JedisPooledを一度だけ定義
    }

    // 月間クオータ（ユーザー単位 / 月300回）
    single<MonthlyQuotaService> {
        RedisMonthlyQuotaService(
            redis = get(),   // JedisPooled
            limit = AppConfig.monthlyLimit // 月のリクエスト上限数
        )
    }

    // Repositories
    single<PlaceQueryPort> { PlaceRepository() }

    // External clients
    single { GooglePlacesApiClient(AppConfig.googleApiKey) }
    single { LineApiClient(AppConfig.channelAccessToken) }

    // Services
    single {
        SearchService(
            googleClient = get(),
            repository = get()
        )
    }

    // UseCase
    single {
        ReplyUseCase(
            searchService = get(),
            sessionStore = get(),
            quotaService = get()
        )
    }

    // LINE
    single { LineSignatureVerifier(AppConfig.channelSecret) }
    single {
        LineWebhookController(
            verifier = get(),
            replyUseCase = get(),
            lineClient = get()
        )
    }
}
