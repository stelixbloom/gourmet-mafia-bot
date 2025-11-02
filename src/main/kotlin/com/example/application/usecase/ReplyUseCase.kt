package com.example.application.usecase

import com.example.application.dto.LineReplyMessageDto
import com.example.domain.port.PlaceQueryPort

/**
 * 受け取ったテキストに応じて返信内容を決めるUseCaseクラス
 */
class ReplyUseCase(
    private val placeQueryPort: PlaceQueryPort
) {

    fun execute(text: String): LineReplyMessageDto {
        val t = text.trim()

        if (t == "検索開始") {
            return LineReplyMessageDto(
                text = "検索したいエリアを「都道府県」+「市区町村」の形式で入力してください。\n（例：東京都渋谷区）"
            )
        }

        val isArea = t.endsWith("区") || t.endsWith("市") || t.endsWith("町") || t.endsWith("村")
        if (isArea) {
            val places = placeQueryPort.findActiveByCity(t, limit = 5)
            return if (places.isEmpty()) {
                LineReplyMessageDto("該当するものがありませんでした。")
            } else {
                val lines = places.joinToString("\n") { "・${it.name}\n${it.shopUrl}" }
                LineReplyMessageDto("おすすめ（$t）：\n$lines")
            }
        } else {
            LineReplyMessageDto("該当するものがありませんでした。")
        }

        return LineReplyMessageDto("「検索開始」からお店を検索してください！")
    }
}
