package com.example.application.usecase

import com.example.application.dto.LineReplyMessageDto
import com.example.application.dto.TextReplyMessageDto
import com.example.application.service.MonthlyQuotaService
import com.example.application.service.SearchService
import com.example.application.session.SearchSession
import com.example.application.session.SessionStore
import com.example.application.session.Step
import com.example.interfaceadapters.line.AreaInput
import com.example.interfaceadapters.line.FlexTemplates

/**
 * 受け取ったテキストに応じて返信内容を決めるUseCaseクラス
 */
class ReplyUseCase(
    private val searchService: SearchService,
    private val sessionStore: SessionStore,
    private val quotaService: MonthlyQuotaService
) {

    // 「戻る」トリガー用キーワード
    private val BACK_KEYWORDS = setOf("前の質問に戻る", "戻る")

    suspend fun execute(userId: String, textRaw: String): LineReplyMessageDto {
        val text = textRaw.trim()

        // ↓チャット開始

        if (text == "問い合わせ") {
            sessionStore.clear(userId)
            return TextReplyMessageDto(
                text = "こちらのメールアドレスへご連絡ください✉️\n「メールアドレス」",
            )
        }

        var session = sessionStore.get(userId)
        if (text == "検索開始" || text == "検索" || session == null) {

            // APIリクエスト上限チェック：チェック
            val key = "user:$userId"
            if (!quotaService.hasRemaining(key)) {
                sessionStore.clear(userId)
                return TextReplyMessageDto(
                    text = "ごめんなさい🙏\n今月の検索回数が上限（300件）に達しました。\n翌月以降にまたご利用ください。"
                )
            }

            session = SearchSession(userId = userId, step = Step.WAIT_AREA)
            sessionStore.save(session)
            return TextReplyMessageDto(
                text = "検索したいエリアを入力してください📍\n（例：東京都 渋谷区 恵比寿 ／ 渋谷駅 ／ 東京 日本橋）",
            )
        }

        // 前の質問に戻る
        if (text in BACK_KEYWORDS) {
            return this.handleBack(session)
        }

        // 「希望エリア」 → 「希望ジャンル（親）」 → 「希望ジャンル（サブ）」 → 「希望価格」 →「 利用シーン」 → 検索（GoogleAPI & DB）
        return when (session.step) {

            // 希望エリアの入力が完了していたら↓
            Step.WAIT_AREA -> {
                val res = AreaInput.sanitize(text)
                if (!res.ok) {
                    TextReplyMessageDto(
                        text = "もう一度検索したいエリアを入力してください📍\n（例：東京都 渋谷区 恵比寿／渋谷駅／東京 日本橋）"
                    )
                } else {
                    val next = session.copy(step = Step.WAIT_GENRE, area = res.value)
                    sessionStore.save(next)
                    FlexTemplates.genreParent()
                }
            }

            // 親ジャンルの入力が完了していたら↓
            Step.WAIT_GENRE -> {
                val parsed = LineUserOptions.parseGenreParent(text)
                if (parsed == null) {
                    // 入力ミス → もう一度同じFlexを出す
                    FlexTemplates.genreParent()
                } else {
                    val (label, _) = parsed
                    val subOptions = LineUserOptions.SUBGENRE_USER_LABELS[label]
                    if (label == "おまかせ" || subOptions.isNullOrEmpty()) {
                        val next = session.copy(step = Step.WAIT_PRICE, genreLabel = label, subgenreLabel = null)
                        sessionStore.save(next)
                        FlexTemplates.price()
                    } else {
                        val next = session.copy(step = Step.WAIT_SUBGENRE, genreLabel = label)
                        sessionStore.save(next)
                        FlexTemplates.genreSub(label, subOptions)
                    }
                }
            }

            // サブジャンルの入力が完了していたら↓
            Step.WAIT_SUBGENRE -> {
                val parent = session.genreLabel
                if (parent == null) {
                    val back = session.copy(step = Step.WAIT_GENRE)
                    sessionStore.save(back)
                    FlexTemplates.genreParent()
                } else {
                    val parsed = LineUserOptions.parseSubgenre(parent, text) // null なら「指定しない」
                    val childLabel = parsed?.first
                    val next = session.copy(step = Step.WAIT_PRICE, subgenreLabel = childLabel)
                    sessionStore.save(next)
                    FlexTemplates.price()
                }
            }

            // 価格帯の入力が完了していたら↓
            Step.WAIT_PRICE -> {
                val parsed = LineUserOptions.parsePrice(text)
                if (parsed == null) {
                    FlexTemplates.price()
                } else {
                    val (label, levels) = parsed
                    val next = session.copy(step = Step.WAIT_HOURS, priceLabel = label, priceLevels = levels)
                    sessionStore.save(next)
                    FlexTemplates.hours()
                }
            }

            // 利用シーンの入力が完了していたら↓
            Step.WAIT_HOURS -> {
                val parsed = LineUserOptions.parseHours(text)
                if (parsed == null) {
                    FlexTemplates.hours()
                } else {
                    val (label, band) = parsed
                    val done = session.copy(hoursLabel = label, hoursBand = band)
                    sessionStore.save(done)

                    // APIリクエスト上限チェック：カウント
                    val key = "user:$userId"
                    if (!quotaService.tryConsume(key)) {
                        sessionStore.clear(userId)
                        return TextReplyMessageDto(
                            text = "ごめんなさい🙏\n今月の検索回数が上限（300件）に達しました。\n翌月以降にまたご利用ください。"
                        )
                    }

                    // ------- Places API 検索 -------
                    val genreToken = this.genreTokenForTextSearch(done.genreLabel, done.subgenreLabel)
                    val results = searchService.search(
                        area        = done.area!!,
                        genreToken  = genreToken,      // サブがあればサブトークン優先
                        priceLevels = done.priceLevels,
                        hoursBand   = done.hoursBand,
                        limit       = 3
                    )
                    sessionStore.clear(userId)

                    if (results.isEmpty()) {
                        TextReplyMessageDto(
                            text = "検索ワード🔍（${done.area} / ${done.genreLabel ?: "おまかせ"} " +
                                    (done.subgenreLabel?.let { "（$it）" } ?: "") +
                                        " / ${done.priceLabel ?: "おまかせ"} / ${done.hoursLabel ?: "おまかせ"}）" +
                                    " \n\n該当するお店がありませんでした。。\n条件を変えてもう一度検索してください😢"
                        )
                    } else {

                        val responseText = StringBuilder()
                        for (result in results) {

                            // 店名
                            responseText.append("⭐️").append(result.name).append('\n')
                            // DBに情報あれば
                            if (result.recommended) {
                                responseText.append("グルメマフィア イチオシのお店😎✨\n")
                            }
                            if (!result.comment.isNullOrBlank()) {
                                responseText.append("   メモ　　: ").append(result.comment).append('\n')
                            }
                            // URL
                            responseText.append(result.googleMapsUri).append('\n').append('\n')
                        }
                        TextReplyMessageDto(
                            text =
                                "検索ワード🔍（${done.area} / ${done.genreLabel ?: "おまかせ"}" +
                                        (done.subgenreLabel?.let { "（$it）" } ?: "") +
                                        " / ${done.priceLabel ?: "おまかせ"} / ${done.hoursLabel ?: "おまかせ"}）\n\nおすすめのお店はこちら！✨\n\n" +
                                        responseText.toString().trimEnd()
                        )
                    }
                }
            }
        }
    }

    /**
     * 前の質問に戻る処理
     */
    private fun handleBack(session: SearchSession?): LineReplyMessageDto {

        if (session == null) {
            // セッションが無いのに戻ろうとしている場合
            return TextReplyMessageDto(
                text = "検索したいエリアを入力してください📍\n（例：東京都 渋谷区 恵比寿 ／ 渋谷駅 ／ 東京 日本橋）"
            )
        }

        return when (session.step) {

            Step.WAIT_AREA -> {
                // すでに一番最初
                TextReplyMessageDto(
                    text = "検索したいエリアを入力してください📍\n（例：東京都 渋谷区 恵比寿 ／ 渋谷駅 ／ 東京 日本橋）"
                )
            }

            Step.WAIT_GENRE -> {
                // エリア入力に戻す
                val next = session.copy(step = Step.WAIT_AREA)
                sessionStore.save(next)
                TextReplyMessageDto(
                    text = "検索したいエリアを入力し直してください📍\n（例：東京都 渋谷区 恵比寿 ／ 渋谷駅 ／ 東京 日本橋）"
                )
            }

            Step.WAIT_SUBGENRE -> {
                // 親ジャンル選択に戻す
                val next = session.copy(step = Step.WAIT_GENRE, subgenreLabel = null)
                sessionStore.save(next)
                FlexTemplates.genreParent()
            }

            Step.WAIT_PRICE -> {
                // 価格 → （サブジャンル or 親ジャンル）へ戻す
                val genre = session.genreLabel
                val hasSubOptions = genre != null &&
                        (LineUserOptions.SUBGENRE_USER_LABELS[genre]?.isNotEmpty() == true)

                val nextStep =
                    if (hasSubOptions && genre != "おまかせ") Step.WAIT_SUBGENRE else Step.WAIT_GENRE

                val next = session.copy(
                    step = nextStep,
                    priceLabel = null,
                    priceLevels = null
                )
                sessionStore.save(next)

                if (nextStep == Step.WAIT_SUBGENRE) {
                    FlexTemplates.genreSub(
                        genre ?: "",
                        LineUserOptions.SUBGENRE_USER_LABELS[genre] ?: emptyList()
                    )
                } else {
                    FlexTemplates.genreParent()
                }
            }

            Step.WAIT_HOURS -> {
                // 利用シーン → 価格へ戻す
                val next = session.copy(
                    step = Step.WAIT_PRICE,
                    hoursLabel = null,
                    hoursBand = null
                )
                sessionStore.save(next)
                FlexTemplates.price()
            }
        }
    }

    /**
     * ユーザーが入力したジャンルを返却
     *
     * サブジャンルがあれば、そのまま返却
     * 親ジャンルしかない場合、親ジャンルを返却
     * 親ジャンルすらない場合、「おまかせ」の場合、nullを返却（親ジャンルは必須になっている想定）
     */
    fun genreTokenForTextSearch(genreLabel: String?, subgenreLabel: String?): String? {
        val parent = genreLabel?.trim().orEmpty()
        val child  = subgenreLabel?.trim().orEmpty()

        if (parent.isEmpty() || parent == "おまかせ") return null

        // サブがあればサブ優先
        if (child.isNotEmpty()) {
            val w = LineUserOptions.SUBGENRE_SEARCH_WORDS[child]
            return (w ?: listOf(child)).joinToString(" ")
        }

        // サブが無ければ親
        val w = LineUserOptions.GENRE_SEARCH_WORDS[parent]
        return (w ?: listOf(parent)).joinToString(" ")
    }
}
