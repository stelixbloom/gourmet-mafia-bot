package com.example.interfaceadapters.line


/**
 * ユーザーが希望エリアを入力した際に、ある程度バリデーションしたいため作成したクラス
 */
object AreaInput {

    // 許容: 漢字/ひらがな/カタカナ/英数/空白/一部記号（・ー‐-_/()'.,）
    private val allowed = Regex("[\\p{IsHan}\\p{InHiragana}\\p{InKatakana}A-Za-z0-9 　・ー‐\\-_/()'.,]+")
    private val multiSpace = Regex("[\\s　]+")

    data class Result(val ok: Boolean, val value: String, val reason: String? = null)

    fun sanitize(raw: String, maxLen: Int = 20): Result {

        var text = raw.trim()

        // 改行・タブは空白化
        text = text.replace(multiSpace, " ")

        if (text.isEmpty()) return Result(false, "", "empty")

        // URLや@は弾く
        if (text.contains("http", ignoreCase = true) || text.contains('@')) {
            return Result(false, "", "url_or_at")
        }

        // SQL演算子っぽい < > ; = * などは許可しない
        if (!allowed.matches(text)) {
            return Result(false, "", "illegal_chars")
        }

        // あまりに長いクエリは切る（API側にも優しい）
        if (text.length > maxLen) {
            text = text.take(maxLen)
        }

        // 連続同一文字（例: ーーーー、aaaaaa）は雑に短縮
        text = text.replace(Regex("(.)\\1{9,}"), "$1$1$1")

        return Result(true, text)
    }
}
