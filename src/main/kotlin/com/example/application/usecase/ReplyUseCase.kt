package com.example.application.usecase

import com.example.application.dto.LineReplyMessageDto
import com.example.application.service.SearchService
import com.example.application.session.SearchSession
import com.example.application.session.SessionStore
import com.example.application.session.Step

/**
 * 受け取ったテキストに応じて返信内容を決めるUseCaseクラス
 */
class ReplyUseCase(
    private val searchService: SearchService,
    private val sessionStore: SessionStore
) {
    suspend fun execute(userId: String, textRaw: String): LineReplyMessageDto {
        val text = textRaw.trim()

        if (text == "問い合わせ") {
            return LineReplyMessageDto(
                text = "こちらのメールアドレスへご連絡ください✉️\n「メールアドレス」",
            )
        }

        // 初期開始
        var session = sessionStore.get(userId)
        if (text == "検索開始" || text == "検索" || session == null) {
            session = SearchSession(userId = userId, step = Step.WAIT_AREA)
            sessionStore.save(session)
            return LineReplyMessageDto(
                text = "検索したいエリアを「都道府県+市区町村」で入力してください📍\n（例：東京都渋谷区）",
            )
        }

        // 希望エリア → 希望ジャンル → 希望価格 → 利用シーン → 検索（GoogleAPI & DB）
        return when (session.step) {

            Step.WAIT_AREA -> {
                val isArea = text.endsWith("区") || text.endsWith("市") || text.endsWith("町") || text.endsWith("村")
                if (!isArea) {
                    LineReplyMessageDto("エリアをもう一度入力してください📍\n（例：東京都渋谷区）")
                } else {
                    val next = session.copy(step = Step.WAIT_GENRE, area = text)
                    sessionStore.save(next)
                    LineReplyMessageDto(
                        text = "ジャンル（大項目）を選択してください🍖🍕🍜",
                        quickReplies = LineUserOptions.GENRE_USER_LABELS.map { it to it }
                    )
                }
            }

            Step.WAIT_GENRE -> {
                val parsed = LineUserOptions.parseGenreParent(text)
                if (parsed == null) {
                    LineReplyMessageDto(
                        text = "もう一度、ジャンル（大項目）を選択してください🍖🍕🍜",
                        quickReplies = LineUserOptions.GENRE_USER_LABELS.map { it to it }
                    )
                } else {
                    val (label, _) = parsed
                    val subOptions = LineUserOptions.SUBGENRE_USER_LABELS[label]
                    if (label == "おまかせ" || subOptions.isNullOrEmpty()) {
                        val next = session.copy(step = Step.WAIT_PRICE, genreLabel = label, subgenreLabel = null)
                        sessionStore.save(next)
                        LineReplyMessageDto(
                            text = "価格帯の目安を選択してください💰",
                            quickReplies = LineUserOptions.PRICE_LABELS.map { it to it }
                        )
                    } else {
                        val next = session.copy(step = Step.WAIT_SUBGENRE, genreLabel = label)
                        sessionStore.save(next)
                        LineReplyMessageDto(
                            text = "小項目を選択してください🔎（指定しないも可）",
                            quickReplies = subOptions.map { it to it }
                        )
                    }
                }
            }

            Step.WAIT_SUBGENRE -> {
                val parent = session.genreLabel
                if (parent == null) {
                    val back = session.copy(step = Step.WAIT_GENRE)
                    sessionStore.save(back)
                    LineReplyMessageDto(
                        text = "ジャンル（大項目）を選択してください🍖🍕🍜",
                        quickReplies = LineUserOptions.GENRE_USER_LABELS.map { it to it }
                    )
                } else {
                    val parsed = LineUserOptions.parseSubgenre(parent, text) // null なら「指定しない」
                    val childLabel = parsed?.first
                    val next = session.copy(step = Step.WAIT_PRICE, subgenreLabel = childLabel)
                    sessionStore.save(next)
                    LineReplyMessageDto(
                        text = "価格帯の目安を選択してください💰",
                        quickReplies = LineUserOptions.PRICE_LABELS.map { it to it }
                    )
                }
            }

            Step.WAIT_PRICE -> {
                val parsed = LineUserOptions.parsePrice(text)
                if (parsed == null) {
                    LineReplyMessageDto(
                        text = "もう一度、価格帯の目安を選択してください💰",
                        quickReplies = LineUserOptions.PRICE_LABELS.map { it to it }
                    )
                } else {
                    val (label, levels) = parsed
                    val next = session.copy(step = Step.WAIT_HOURS, priceLabel = label, priceLevels = levels)
                    sessionStore.save(next)
                    LineReplyMessageDto(
                        text = "利用シーンを選択してください☀️🌙",
                        quickReplies = LineUserOptions.HOURS_LABELS.map { it to it }
                    )
                }
            }

            Step.WAIT_HOURS -> {
                val parsed = LineUserOptions.parseHours(text)
                if (parsed == null) {
                    LineReplyMessageDto(
                        text = "もう一度、利用シーンを選んでください☀️🌙",
                        quickReplies = LineUserOptions.HOURS_LABELS.map { it to it }
                    )
                } else {
                    val (label, band) = parsed
                    val done = session.copy(hoursLabel = label, hoursBand = band)
                    sessionStore.save(done)

                    // ------- Places API 検索 -------
                    val genreToken = genreTokenForTextSearch(done.genreLabel, done.subgenreLabel)
                    val results = searchService.search(
                        area        = done.area!!,
                        genreToken  = genreToken,      // 子があれば子トークン優先
                        priceLevels = done.priceLevels,
                        hoursBand   = done.hoursBand,
                        limit       = 5
                    )
                    sessionStore.clear(userId)

                    if (results.isEmpty()) {
                        LineReplyMessageDto(text = "ごめんなさい、該当するお店がありませんでした。。")
                    } else {
                        val lines = results.joinToString("\n") { r ->
                            val memo = r.comment?.takeIf { it.isNotBlank() }?.let { "（メモ: $it）" } ?: ""
                            "⭐️${r.name}$memo\n${r.googleMapsUri}"
                        }
                        LineReplyMessageDto(
                            text =
                                "おすすめ（${done.area} / ${done.genreLabel ?: "おまかせ"}" +
                                        (done.subgenreLabel?.let { "（$it）" } ?: "") +
                                        " / ${done.priceLabel ?: "おまかせ"} / ${done.hoursLabel ?: "おまかせ"}）：\n$lines"
                        )
                    }
                }
            }
        }
    }

    /**
     * ユーザーが入力したジャンル（親、サブ）の文字列結合
     */
    fun genreTokenForTextSearch(genreLabel: String?, subgenreLabel: String?): String? {
        val parent = genreLabel?.trim().orEmpty()
        val child  = subgenreLabel?.trim().orEmpty()

        if (parent.isEmpty() || parent == "おまかせ") return null

        // 子があれば子優先
        if (child.isNotEmpty()) {
            val w = LineUserOptions.SUBGENRE_SEARCH_WORDS[child]
            return (w ?: listOf(child)).joinToString(" ")
        }

        // 子が無ければ親
        val w = LineUserOptions.GENRE_SEARCH_WORDS[parent]
        return (w ?: listOf(parent)).joinToString(" ")
    }
}
