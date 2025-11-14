package com.example.application.session

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPooled

class  RedisSessionStore(redisUrl: String): SessionStore {
    private val client = JedisPooled(redisUrl)

    private fun key(id: String) = "session:$id"

    override fun get(userId: String): SearchSession? {
        val s = client.get(key(userId)) ?: return null
        return Json.decodeFromString<SearchSession>(s)
    }

    override fun save(session: SearchSession, ttlSeconds: Int) {
        val s = Json.encodeToString(session)
        client.setex(key(session.userId), ttlSeconds.toLong(), s)
    }

    override fun clear(userId: String) {
        client.del(key(userId))
    }
}
