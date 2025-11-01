package com.example.application.usecase

import com.example.application.dto.ReplyMessage
import com.example.domain.port.PlaceQueryPort

/**
 * 受け取ったテキストに応じて返信内容を決めるUseCaseクラス
 */
class ReplyUseCase(
    private val placeQueryPort: PlaceQueryPort
) {

    fun execute(text: String): ReplyMessage {
        val t = text.trim()

        if (t == "検索開始") {
            return ReplyMessage(
                text = "検索したいエリアを「都道府県」+「市区町村」の形式で入力してください。（例：東京都渋谷区）"
            )
        }

        val isArea = t.endsWith("区") || t.endsWith("市") || t.endsWith("町") || t.endsWith("村")
        if (isArea) {
            val places = placeQueryPort.findActiveByCity(t, limit = 5)
            return if (places.isEmpty()) {
                ReplyMessage("該当するものがありませんでした。")
            } else {
                val lines = places.joinToString("\n") { "・${it.name}\n${it.shopUrl}" }
                ReplyMessage("おすすめ（$t）：\n$lines")
            }
        }

        return ReplyMessage("「検索開始」からお店を検索してください！")
    }
}
