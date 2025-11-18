package com.example.application.dto

import kotlinx.serialization.json.JsonElement

/**
 * LINEへの返信フォーマット
 */
sealed interface LineReplyMessageDto

data class TextReplyMessageDto(
    val text: String,
    val quickReplies: List<Pair<String, String>>? = null
) : LineReplyMessageDto

data class FlexReplyMessageDto(
    val altText: String,
    val contents: JsonElement
) : LineReplyMessageDto
