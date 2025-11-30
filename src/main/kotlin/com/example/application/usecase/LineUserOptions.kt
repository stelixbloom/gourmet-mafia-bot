package com.example.application.usecase

import com.example.domain.model.HoursBand

object LineUserOptions {

    // 親ジャンル
    val GENRE_USER_LABELS = listOf(
        "和食",
        "洋食・フレンチ・イタリアン",
        "中華・アジアエスニック",
        "肉料理",
        "ラーメン・カレー・粉物",
        "スイーツ・パン・カフェ",
        "居酒屋・バー"
    )

    // 親ジャンル（サブジャンルが入力なければ↓のワードが使われる想定）
    val GENRE_SEARCH_WORDS: Map<String, List<String>> = mapOf(
        "和食" to listOf("和食"),
        "洋食・フレンチ・イタリアン" to listOf("洋食"),
        "中華・アジアエスニック" to listOf("中華", "エスニック"),
        "肉料理" to listOf("肉"),
        "ラーメン・カレー・粉物" to listOf("粉物"),
        "スイーツ・パン・カフェ" to listOf("スイーツ", "カフェ"),
        "居酒屋・バー" to listOf("居酒屋", "バー")
    )

    // 親 → サブ候補リスト
    // 指定しないを含む
    val SUBGENRE_USER_LABELS: Map<String, List<String>> = mapOf(
        "和食" to listOf("指定しない", "寿司", "天ぷら", "うなぎ", "蕎麦", "うどん", "定食", "焼き鳥"),
        "洋食・フレンチ・イタリアン" to listOf("指定しない", "洋食", "フレンチ", "イタリアン", "ピザ", "パスタ"),
        "中華・アジアエスニック" to listOf("指定しない", "中華", "韓国", "タイ", "ベトナム", "インド", "ネパール"),
        "肉料理" to listOf("指定しない", "焼肉", "ハンバーグ", "ステーキ", "とんかつ", "焼き鳥", "ジンギスカン"),
        "ラーメン・カレー・粉物" to listOf("指定しない", "ラーメン", "つけ麺", "カレー", "お好み焼き", "たこ焼き", "もんじゃ"),
        "スイーツ・パン・カフェ" to listOf("指定しない", "カフェ", "ケーキ", "パフェ", "パン"),
        "居酒屋・バー" to listOf("指定しない", "居酒屋", "バー", "ワインバー", "ビアバー")
    )

    // サブジャンル
    val SUBGENRE_SEARCH_WORDS: Map<String, List<String>> = mapOf(
        "寿司" to listOf("寿司"),
        "天ぷら" to listOf("天ぷら"),
        "うなぎ" to listOf("鰻"),
        "蕎麦" to listOf("そば"),
        "うどん" to listOf("うどん"),
        "定食" to listOf("定食"),
        "焼き鳥" to listOf("焼き鳥"),

        "洋食" to listOf("洋食"),
        "フレンチ" to listOf("フレンチ"),
        "イタリアン" to listOf("イタリアン"),
        "ピザ" to listOf("ピザ"),
        "パスタ" to listOf("パスタ"),

        "中華" to listOf("中華"),
        "韓国" to listOf("韓国料理"),
        "タイ" to listOf("タイ料理"),
        "ベトナム" to listOf("ベトナム料理"),
        "インド" to listOf("インド料理"),
        "ネパール" to listOf("ネパール料理"),

        "焼肉" to listOf("焼肉"),
        "ハンバーグ" to listOf("ハンバーグ"),
        "ステーキ" to listOf("ステーキ"),
        "とんかつ" to listOf("とんかつ"),
        "ジンギスカン" to listOf("ジンギスカン"),

        "ラーメン" to listOf("ラーメン"),
        "つけ麺" to listOf("つけ麺"),
        "カレー" to listOf("カレー"),
        "お好み焼き" to listOf("お好み焼き"),
        "たこ焼き" to listOf("たこ焼き"),
        "もんじゃ" to listOf("もんじゃ焼き"),

        "カフェ" to listOf("カフェ"),
        "ケーキ" to listOf("ケーキ"),
        "パフェ" to listOf("パフェ"),
        "パン" to listOf("パン"),

        "居酒屋" to listOf("居酒屋"),
        "バー" to listOf("バー"),
        "ワインバー" to listOf("ワインバー"),
        "ビアバー" to listOf("ビアバー")
    )

    // 親ジャンルのパース
    fun parseGenreParent(input: String): Pair<String, Set<String>?>? = when (input) {
        "おまかせ" -> "おまかせ" to null
        in GENRE_USER_LABELS -> {
            val words = GENRE_SEARCH_WORDS[input] ?: listOf(input)
            input to words.toSet()
        }
        else -> null
    }

    // 子ジャンルのパース（「指定しない」なら null を返す）
    fun parseSubgenre(parent: String, input: String): Pair<String?, Set<String>?>? {
        val options = SUBGENRE_USER_LABELS[parent] ?: return null
        if (input == "指定しない") return null  // サブ指定なしの場合親だけで検索
        if (input !in options) return null
        val words = SUBGENRE_SEARCH_WORDS[input] ?: listOf(input)
        return input to words.toSet()
    }

    // 価格
    val PRICE_LABELS = listOf("おまかせ", "安い", "カジュアル", "高め")
    fun parsePrice(input: String): Pair<String, Set<Int>?>? = when (input) {
        "おまかせ" -> "おまかせ" to null
        "安い" -> "安い" to setOf(0, 1)
        "カジュアル" -> "カジュアル" to setOf(2)
        "高め" -> "高め" to setOf(3, 4)   // ← 両方まとめる
        else -> null
    }

    // 利用シーン
    val HOURS_LABELS = listOf("おまかせ", "モーニングタイム", "ランチタイム", "ディナータイム")
    fun parseHours(input: String): Pair<String, HoursBand?>? = when (input) {
        "おまかせ" -> "おまかせ" to null
        "モーニングタイム" -> "モーニング" to HoursBand.MORNING
        "ランチタイム" -> "ランチ" to HoursBand.LUNCH
        "ディナータイム" -> "ディナー" to HoursBand.DINNER
        else -> null
    }
}
