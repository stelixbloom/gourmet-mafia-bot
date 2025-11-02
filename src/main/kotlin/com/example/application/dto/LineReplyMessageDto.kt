package com.example.application.dto

/**
 * LINEへの返信フォーマット
 */
data class LineReplyMessageDto(
    val text: String,
    val quickReplies: List<Pair<String, String>>? = null // (label, text to send)
)
