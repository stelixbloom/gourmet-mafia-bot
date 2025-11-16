package com.example.application.usecase

import com.example.domain.model.HoursBand

object LineUserOptions {

    // 親ジャンル
    val GENRE_USER_LABELS = listOf(
        "おまかせ",
        "和食系",
        "洋食系",
        "アジア・エスニック系",
        "肉料理",
        "粉物系",
        "カレー",
        "スイーツ・カフェ系",
        "居酒屋・バー",
        "麺類"
    )

    // 親 → サブ候補リスト
    // 指定しないを含む
    val SUBGENRE_USER_LABELS: Map<String, List<String>> = mapOf(
        "和食系" to listOf("指定しない", "寿司", "天ぷら", "うなぎ", "蕎麦", "うどん", "定食", "焼き鳥"),
        "洋食系" to listOf("指定しない", "イタリアン", "フレンチ", "ステーキ", "ハンバーグ", "パスタ"),
        "アジア・エスニック系" to listOf("指定しない", "中華", "韓国", "タイ", "ベトナム", "インド", "ネパール"),
        "肉料理" to listOf("指定しない", "焼肉", "ステーキ", "とんかつ", "焼き鳥", "ジンギスカン"),
        "粉物系" to listOf("指定しない", "お好み焼き", "たこ焼き", "もんじゃ"),
        "カレー" to listOf("指定しない", "日式カレー", "インドカレー", "スープカレー"),
        "スイーツ・カフェ系" to listOf("指定しない", "カフェ", "ケーキ", "パフェ", "パン"),
        "居酒屋・バー" to listOf("指定しない", "居酒屋", "バー", "ワインバー", "ビアバー"),
        "麺類" to listOf("指定しない", "ラーメン", "蕎麦", "うどん", "つけ麺")
    )

    // 親ジャンル
    val GENRE_SEARCH_WORDS: Map<String, List<String>> = mapOf(
        "和食系" to listOf("和食"),
        "洋食系" to listOf("洋食"),
        "アジア・エスニック系" to listOf("エスニック"),
        "肉料理" to listOf("肉"),
        "粉物系" to listOf("粉物"),
        "カレー" to listOf("カレー"),
        "スイーツ・カフェ系" to listOf("スイーツ", "カフェ"),
        "居酒屋・バー" to listOf("居酒屋", "バー"),
        "麺類" to listOf("麺類")
    )

    // サブジャンル
    val SUBGENRE_SEARCH_WORDS: Map<String, List<String>> = mapOf(
        "寿司" to listOf("寿司"),
        "天ぷら" to listOf("天ぷら"),
        "うなぎ" to listOf("うなぎ"),
        "蕎麦" to listOf("そば"),
        "うどん" to listOf("うどん"),
        "定食" to listOf("定食"),
        "焼き鳥" to listOf("焼き鳥"),

        "イタリアン" to listOf("イタリアン"),
        "フレンチ" to listOf("フレンチ"),
        "ステーキ" to listOf("ステーキ"),
        "ハンバーグ" to listOf("ハンバーグ"),
        "パスタ" to listOf("パスタ"),

        "中華" to listOf("中華"),
        "韓国" to listOf("韓国料理"),
        "タイ" to listOf("タイ料理"),
        "ベトナム" to listOf("ベトナム料理"),
        "インド" to listOf("インド料理"),
        "ネパール" to listOf("ネパール料理"),

        "焼肉" to listOf("焼肉"),
        "とんかつ" to listOf("とんかつ"),
        "ジンギスカン" to listOf("ジンギスカン"),

        "お好み焼き" to listOf("お好み焼き"),
        "たこ焼き" to listOf("たこ焼き"),
        "もんじゃ" to listOf("もんじゃ焼き"),

        "日式カレー" to listOf("カレー"),
        "インドカレー" to listOf("インドカレー"),
        "スープカレー" to listOf("スープカレー"),

        "カフェ" to listOf("カフェ"),
        "ケーキ" to listOf("ケーキ"),
        "パフェ" to listOf("パフェ"),
        "パン" to listOf("ベーカリー", "パン"),

        "居酒屋" to listOf("居酒屋"),
        "バー" to listOf("バー"),
        "ワインバー" to listOf("ワインバー"),
        "ビアバー" to listOf("ビアバー"),

        "ラーメン" to listOf("ラーメン"),
        "つけ麺" to listOf("つけ麺")
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
        if (input == "指定しない") return null  // サブ指定なし＝親だけで検索
        if (input !in options) return null
        val words = SUBGENRE_SEARCH_WORDS[input] ?: listOf(input)
        return input to words.toSet()
    }

    // 価格
    val PRICE_LABELS = listOf("おまかせ", "安い", "カジュアル", "やや高め", "お高め")
    fun parsePrice(input: String): Pair<String, Set<Int>?>? = when (input) {
        "おまかせ" -> "おまかせ" to null
        "安い" -> "安い" to setOf(0, 1)
        "カジュアル" -> "カジュアル" to setOf(2)
        "やや高め" -> "やや高め" to setOf(3)
        "お高め" -> "お高め" to setOf(4)
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
