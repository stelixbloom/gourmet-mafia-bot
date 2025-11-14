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

        // TODO 問い合わせ

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

        // 状態機械：エリア → ジャンル → 価格 → 利用シーン → 検索
        return when (session.step) {

            Step.WAIT_AREA -> {
                val isArea = text.endsWith("区") || text.endsWith("市") || text.endsWith("町") || text.endsWith("村")
                if (!isArea) {
                    LineReplyMessageDto("エリアをもう一度入力してください。\n（例：東京都渋谷区）")
                } else {
                    val next = session.copy(step = Step.WAIT_GENRE, city = text)
                    sessionStore.save(next)
                    LineReplyMessageDto(
                        text = "ジャンルを選択してください🍖🍕🍜",
                        quickReplies = LineUserOptions.GENRE_LABELS.map { it to it }
                    )
                }
            }

            Step.WAIT_GENRE -> {
                val parsed = LineUserOptions.parseGenre(text)
                if (parsed == null) {
                    LineReplyMessageDto(
                        text = "もう一度、ジャンルを選択してください。",
                        quickReplies = LineUserOptions.GENRE_LABELS.map { it to it }
                    )
                } else {
                    val (label, tags) = parsed
                    val next = session.copy(step = Step.WAIT_PRICE, genreLabel = label, genreTags = tags)
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
                        text = "もう一度、価格帯の目安を選択してください。",
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
                        text = "もう一度、利用シーンを選んでください。",
                        quickReplies = LineUserOptions.HOURS_LABELS.map { it to it }
                    )
                } else {
                    val (label, band) = parsed
                    val done = session.copy(hoursLabel = label, hoursBand = band)
                    sessionStore.save(done)

                    // ------- Places API 検索 -------
                    val genreToken = genreTokenForTextSearch(done.genreLabel) // おまかせの場合 null
                    val results = searchService.search(
                        area        = done.city!!,
                        genreToken  = genreToken,
                        priceLevels = done.priceLevels,
                        hoursBand   = done.hoursBand,
                        limit       = 5
                    )
                    sessionStore.clear(userId)

                    if (results.isEmpty()) {
                        LineReplyMessageDto(
                            text = "該当がありませんでした。最初から検索しますか？",
                        )
                    } else {
                        // テーブル取得時のみcommentあり
                        val lines = results.joinToString("\n") { r ->
                            val memo = r.comment?.takeIf { it.isNotBlank() }?.let { "（メモ: $it）" } ?: ""
                            "・${r.name}$memo\n${r.googleMapsUri}"
                        }
                        LineReplyMessageDto(
                            text =
                                "おすすめ（${done.city} / ${done.genreLabel ?: "おまかせ"} / " +
                                        "${done.priceLabel ?: "おまかせ"} / ${done.hoursLabel ?: "おまかせ"}）：\n$lines",
                        )
                    }
                }
            }
        }
    }

    /**
     * TextSearch に足すジャンルキーワードを返す。
     * 例: 「ラーメン」「カレー」「カフェ」など。おまかせは null を返す。 TODO おまかせパターンと、ジャンルの幅広げる（検索ワード少なく）
     */
    private val GENRE_WORDS: Map<String, List<String>> = mapOf(
        "和食系" to listOf("和食"),
        "洋食系" to listOf("イタリアン"),
        "アジア・エスニック系" to listOf("中華"),
        "肉料理・粉物系" to listOf("焼肉"),
        "カレー" to listOf("カレー"),
        "スイーツ・カフェ系" to listOf("カフェ"),
        "居酒屋・バー" to listOf("居酒屋"),
        "麺類" to listOf("ラーメン")
//        "和食系" to listOf("和食", "寿司", "天ぷら", "そば", "うどん", "とんかつ", "焼鳥"),
//        "洋食系" to listOf("イタリアン", "フレンチ", "ビストロ", "ピザ", "パスタ", "ステーキ", "ハンバーガー"),
//        "アジア・エスニック系" to listOf("中華", "台湾", "タイ", "ベトナム", "韓国", "インド", "ネパール"),
//        "肉料理・粉物系" to listOf("焼肉", "ステーキ", "ハンバーグ", "シュラスコ", "お好み焼き", "たこ焼き", "もんじゃ"),
//        "カレー" to listOf("カレー", "スパイスカレー", "インドカレー", "タイカレー"),
//        "スイーツ・カフェ系" to listOf("カフェ", "喫茶", "珈琲", "パティスリー", "ケーキ", "ジェラート", "ベーカリー"),
//        "居酒屋・バー" to listOf("居酒屋", "立ち飲み", "ワインバー", "ビアバー", "クラフトビール", "バー"),
//        "麺類" to listOf("ラーメン", "つけ麺", "油そば", "うどん", "そば", "パスタ", "フォー")
    )

    private fun genreTokenForTextSearch(genreLabel: String?): String? {
        val label = genreLabel?.trim().orEmpty()
        if (label.isEmpty() || label == "おまかせ") return null
        return GENRE_WORDS[label]?.joinToString(" ") ?: label
    }
}
