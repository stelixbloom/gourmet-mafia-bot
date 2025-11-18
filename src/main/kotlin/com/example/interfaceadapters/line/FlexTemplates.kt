package com.example.interfaceadapters.line

import com.example.application.dto.FlexReplyMessageDto
import com.example.application.usecase.LineUserOptions
import kotlinx.serialization.json.*

object FlexTemplates {

    fun buttonsBubble(
        title: String,
        subtitle: String,
        labels: List<String>
    ): JsonObject = buildJsonObject {
        put("type", "bubble")
        put("size", "mega")
        putJsonObject("body") {
            put("type", "box")
            put("layout", "vertical")
            put("spacing", "md")
            putJsonArray("contents") {
                addJsonObject {
                    put("type", "text")
                    put("text", title)
                    put("weight", "bold")
                    put("size", "lg")
                }
                addJsonObject {
                    put("type", "text")
                    put("text", subtitle)
                    put("size", "sm")
                    put("color", "#666666")
                }
                addJsonObject {
                    put("type", "box")
                    put("layout", "vertical")
                    put("margin", "md")
                    put("spacing", "sm")
                    putJsonArray("contents") {
                        labels.forEach { label ->
                            addJsonObject {
                                put("type", "button")
                                put("style", "secondary")
                                putJsonObject("action") {
                                    put("type", "message")
                                    put("label", label)
                                    put("text", label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun genreParent(): FlexReplyMessageDto =
        FlexReplyMessageDto(
            altText = "希望ジャンル（大項目）を選択してください🔎",
            contents = buttonsBubble(
                title = "何が食べたいですか？🍽",
                subtitle = "希望ジャンル（大項目）を選択してください🔎",
                labels = LineUserOptions.GENRE_USER_LABELS
            )
        )

    fun genreSub(parent: String, labels: List<String>): FlexReplyMessageDto =
        FlexReplyMessageDto(
            altText = "希望ジャンル（小項目）を選択してください🍖🍕🍜",
            contents = buttonsBubble(
                title = "ジャンル（小項目）: $parent",
                subtitle = "お好みのカテゴリを選択してください🍖🍕🍜",
                labels = labels
            )
        )

    fun price(): FlexReplyMessageDto =
        FlexReplyMessageDto(
            altText = "価格帯の目安を選択してください💰",
            contents = buttonsBubble(
                title = "どのくらいの価格帯？💰",
                subtitle = "だいたいの予算感を選んでください",
                labels = LineUserOptions.PRICE_LABELS
            )
        )

    fun hours(): FlexReplyMessageDto =
        FlexReplyMessageDto(
            altText = "利用シーンを選択してください☀️🌙",
            contents = buttonsBubble(
                title = "いつ使いたいですか？☀️🌙",
                subtitle = "利用シーンを選択してください",
                labels = LineUserOptions.HOURS_LABELS
            )
        )
}
