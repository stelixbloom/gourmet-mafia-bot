package com.example.application.dto

/**
 * LINEへの返信フォーマット
 */
data class ReplyMessage(
    val text: String,
    val quickReplies: List<Pair<String, String>>? = null // (label, text to send)
)
