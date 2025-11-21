package com.example.application.service

import redis.clients.jedis.JedisPooled
import java.time.LocalDateTime
import java.time.YearMonth

class RedisMonthlyQuotaService(
    private val redis: JedisPooled,
    private val limit: Int = 3
) : MonthlyQuotaService {

    private fun counterKey(userKey: String, ym: YearMonth): String {
        // キーの形式：quota:user:UserID:YYYY-MM
        return "quota:$userKey:${ym.year}-${ym.monthValue}"
    }

    /**
     * 月の使用回数をチェックする
     */
    override fun hasRemaining(key: String, now: LocalDateTime): Boolean {
        val ym = YearMonth.from(now)
        val redisKey = counterKey(key, ym)
        val used = redis.get(redisKey)?.toIntOrNull() ?: 0
        return used < limit
    }

    /**
     * 1カウント（1検索）増やす
     */
    override fun tryConsume(key: String, now: LocalDateTime): Boolean {
        val ym = YearMonth.from(now)
        val redisKey = counterKey(key, ym)

        // value：（INCR）
        val used = redis.incr(redisKey).toInt()

        // 初回だけTTLセット（40日くらいで削除。月替わりのタイミングでキーが変わるので問題ない想定）
        if (used == 1) {
            redis.expire(redisKey, 60 * 60 * 24 * 40)
        }

        return used <= limit
    }
}