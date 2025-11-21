package com.example.application.service

import java.time.LocalDateTime

interface MonthlyQuotaService {

    /**
     * 今月の残りがあるかだけ判定（カウントは増やさない）
     * true：まだ検索してOK
     * false：上限到達
     */
    fun hasRemaining(key: String, now: LocalDateTime = LocalDateTime.now()): Boolean

    /**
     * カウントを1消費する。
     * true：1消費
     * false：上限
     */
    fun tryConsume(key: String, now: LocalDateTime = LocalDateTime.now()): Boolean
}
