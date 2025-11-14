package com.example.application.usecase

object GenreForGoogleApi {

    // Text Search用
    private val MAP = mapOf(
        "和食系" to listOf("和食", "寿司", "天ぷら", "そば", "うどん", "とんかつ", "焼鳥"),
        "洋食系" to listOf("イタリアン", "フレンチ", "ビストロ", "ピザ", "パスタ", "ステーキ", "ハンバーガー"),
        "アジア・エスニック系" to listOf("中華", "台湾", "タイ", "ベトナム", "韓国", "インド", "ネパール"),
        "肉料理・粉物系" to listOf("焼肉", "ステーキ", "ハンバーグ", "シュラスコ", "お好み焼き", "たこ焼き", "もんじゃ"),
        "カレー" to listOf("カレー", "スパイスカレー", "インドカレー", "タイカレー"),
        "スイーツ・カフェ系" to listOf("カフェ", "喫茶", "珈琲", "パティスリー", "ケーキ", "ジェラート", "ベーカリー"),
        "居酒屋・バー" to listOf("居酒屋", "立ち飲み", "ワインバー", "ビアバー", "クラフトビール", "バー"),
        "麺類" to listOf("ラーメン", "つけ麺", "油そば", "うどん", "そば", "パスタ", "フォー")
    )

    /**
     * UIラベルから Text Search の追加クエリ語を返す。
     * - "おまかせ" / null / 空文字 → null（= ジャンル語は付けない）
     * - 定義が無ければ、そのままラベル文字列を返す（緊急フォールバック）
     */
    fun forTextSearch(genreLabel: String?): String? {
        val label = genreLabel?.trim().orEmpty()
        if (label.isEmpty() || label == "おまかせ") return null
        val tokens = MAP[label]
        return (tokens ?: listOf(label)).joinToString(" ")
    }
}
