package com.example.di

import com.example.application.service.SearchService
import com.example.application.usecase.ReplyUseCase
import com.example.config.AppConfig
import com.example.domain.port.PlaceQueryPort
import com.example.dbaccess.PlaceRepository
import com.example.interfaceadapters.googleplaces.PlacesApiClient
import com.example.application.session.RedisSessionStore
import com.example.application.session.SessionStore
import com.example.interfaceadapters.line.LineApiClient
import com.example.interfaceadapters.line.LineSignatureVerifier
import com.example.interfaceadapters.line.LineWebhookController
import org.koin.dsl.module
import redis.clients.jedis.JedisPooled

val appModule = module {

    // Redis クライアントを全体で共有
    single {
        JedisPooled(AppConfig.redisUrl)
    }

    // Repositories
    single<PlaceQueryPort> { PlaceRepository() }

    // External clients
    single { PlacesApiClient(AppConfig.googleApiKey) }
    single { LineApiClient(AppConfig.channelAccessToken) }

    // SessionStore（Redis 版）
    single<SessionStore> {
        RedisSessionStore(get())   // get() = JedisPooled
    }

    // UseCases / Services
    single { SearchService(get(), get()) }  // PlacesApiClient + PlaceQueryPort
    single { ReplyUseCase(get(), get(), get()) }   // PlaceQueryPort + MonthlyQuotaService + SessionStore

    // LINE
    single { LineSignatureVerifier(AppConfig.channelSecret) }
    single { LineWebhookController(get(), get(), get()) } // verifier, useCase, lineClient
}
