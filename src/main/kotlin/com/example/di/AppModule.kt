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

val appModule = module {

    // Repositories
    single<PlaceQueryPort> { PlaceRepository() }

    // External clients
    single { PlacesApiClient(AppConfig.googleApiKey) }
    single { LineApiClient(AppConfig.channelAccessToken) }

    // Session
    single<SessionStore> { RedisSessionStore(AppConfig.redisUrl) }
//    single { RedisSessionStore(AppConfig.redisUrl) }

    // UseCases / Services
    single { SearchService(get(), get()) }  // PlacesApiClient + PlaceQueryPort
    single { ReplyUseCase(get(), get()) }   // PlaceQueryPort + SessionStore

    // LINE
    single { LineSignatureVerifier(AppConfig.channelSecret) }
    single { LineWebhookController(get(), get(), get()) } // verifier, useCase, lineClient
}
