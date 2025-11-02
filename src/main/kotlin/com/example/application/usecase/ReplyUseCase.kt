package com.example.application.usecase

import com.example.application.dto.LineReplyMessageDto
import com.example.application.session.SearchSession
import com.example.application.session.SessionStore
import com.example.application.session.Step
import com.example.domain.model.SearchCriteria
import com.example.domain.port.PlaceQueryPort

/**
 * 受け取ったテキストに応じて返信内容を決めるUseCaseクラス
 */
class ReplyUseCase(
    private val placeQueryPort: PlaceQueryPort,
    private val sessionStore: SessionStore
) {
    fun execute(userId: String, incomingText: String): LineReplyMessageDto {
        val text = incomingText.trim()

        var session = sessionStore.get(userId)
        if (text == "検索開始" || session == null) {
            session = SearchSession(userId = userId, step = Step.WAIT_AREA)
            sessionStore.save(session)
            return LineReplyMessageDto(
                text = "検索したいエリアを「都道府県+市区町村」で入力してください。\n（例：東京都渋谷区）",
            )
        }

        return when (session.step) {
            Step.WAIT_AREA -> {
                // エリア判定
                val isArea = text.endsWith("区") || text.endsWith("市") || text.endsWith("町") || text.endsWith("村")
                if (!isArea) {
                    LineReplyMessageDto("エリアをもう一度入力してください。\n（例：東京都渋谷区）")
                } else {
                    val next = session.copy(step = Step.WAIT_GENRE, city = text)
                    sessionStore.save(next)
                    LineReplyMessageDto(
                        text = "ジャンルを選択してください。",
                        quickReplies = Options.GENRE_LABELS.map { it to it }
                    )
                }
            }

            Step.WAIT_GENRE -> {
                val parsed = Options.parseGenre(text)
                if (parsed == null) {
                    LineReplyMessageDto(
                        text = "もう一度、ジャンルを選択してください。",
                        quickReplies = Options.GENRE_LABELS.map { it to it }
                    )
                } else {
                    val (label, tags) = parsed
                    val next = session.copy(step = Step.WAIT_PRICE, genreLabel = label, genreTags = tags)
                    sessionStore.save(next)
                    LineReplyMessageDto(
                        text = "価格帯の目安を選択してください。",
                        quickReplies = Options.PRICE_LABELS.map { it to it }
                    )
                }
            }

            Step.WAIT_PRICE -> {
                val parsed = Options.parsePrice(text)
                if (parsed == null) {
                    LineReplyMessageDto(
                        text = "もう一度、価格帯の目安を選択してください。",
                        quickReplies = Options.PRICE_LABELS.map { it to it }
                    )
                } else {
                    val (label, levels) = parsed
                    val next = session.copy(step = Step.WAIT_HOURS, priceLabel = label, priceLevels = levels)
                    sessionStore.save(next)
                    LineReplyMessageDto(
                        text = "利用シーンを選んでください。",
                        quickReplies = Options.HOURS_LABELS.map { it to it }
                    )
                }
            }

            Step.WAIT_HOURS -> {
                val parsed = Options.parseHours(text)
                if (parsed == null) {
                    LineReplyMessageDto(
                        text = "もう一度、利用シーンを選んでください。を選んでください。",
                        quickReplies = Options.HOURS_LABELS.map { it to it }
                    )
                } else {
                    val (label, band) = parsed
                    val done = session.copy(hoursLabel = label, hoursBand = band)
                    sessionStore.save(done)

                    // 検索実行
                    val results = placeQueryPort.findByCriteria(
                        SearchCriteria(
                            city = done.city!!,
                            genreTags = done.genreTags,       // DB対応済みなら活用、未対応なら無視の実装でOK
                            priceLevels = done.priceLevels,   // 同上
                            hoursBand = done.hoursBand        // 同上
                        ),
                        limit = 5
                    )
                    sessionStore.clear(userId) // 1検索1セッションで終了

                    if (results.isEmpty()) {
                        LineReplyMessageDto(
                            text = "該当するものがありませんでした。",
                        )
                    } else {
                        val lines = results.joinToString("\n") { "・${it.name}\n${it.shopUrl}" }
                        LineReplyMessageDto(
                            text = "おすすめ（${done.city} / ${done.genreLabel ?: "おまかせ"} / ${done.priceLabel ?: "おまかせ"} / ${done.hoursLabel ?: "おまかせ"}）：\n$lines",
                        )
                    }
                }
            }
        }
    }
}