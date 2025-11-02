package com.example.application.session

interface SessionStore {
    fun get(userId: String): SearchSession?
    fun save(session: SearchSession, ttlSeconds: Int = 60)
    fun clear(userId: String)
}