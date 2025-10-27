package com.example.interfaceadapters

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

//「LINEに送るメッセージ」の基本的型。TextMsg や TextWithQuick など、異なる形式のメッセージを統一的に扱うためのベース
@Serializable
sealed interface LineMessage

// 実際にAPIに送るJSON例
@Serializable
@SerialName("text")
data class TextMsg(
    val type: String = "text",
    val text: String
): LineMessage

// LINEの「クイックリプライ」ボタン
@Serializable
data class QuickAction(
    val type: String = "message",
    val label: String,
    val text: String)

// LINEの「クイックリプライ」ボタン
@Serializable
data class QuickReplyItem(
    val type: String = "action",
    val action: QuickAction)

// LINEの「クイックリプライ」ボタン
@Serializable
data class QuickReply(
    val items: List<QuickReplyItem>)

//「どのエリア探しますか？」のメッセージ＋ボタン一覧
@Serializable
@SerialName("text_with_quick")
data class TextWithQuick(
    val type: String = "text",
    val text: String,
    val quickReply: QuickReply? = null
): LineMessage

// API に送信するリクエスト全体のJSON構造。「replyToken」はLINEがWebhookで送ってくる「返信対象トーク」の識別子
@Serializable
data class ReplyBody(
    val replyToken: String,
    val messages: List<LineMessage>)

// LineApiClientクラス
class LineApiClient(private val channelAccessToken: String) {
    // Ktorの非同期HTTPクライアント
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                classDiscriminator = "kind" // ← コレが超重要
            })
        }
    }


    suspend fun replyText(
        replyToken: String, // LINE側がWebhookで送ってきた「返信対象メッセージ」のトーク識別子
        text: String,// 返信テキスト
        quick: List<Pair<String,String>>? = null)// クイックリプライボタンのラベルと送信テキストのペア
    {
        // クイックリプライがある場合は TextWithQuick、ない場合は TextMsg
        val msg: LineMessage =
            if (quick.isNullOrEmpty()) TextMsg(text = text)
            else TextWithQuick(
                text = text,
                quickReply = QuickReply(quick.map { (label, t) ->
                    QuickReplyItem(action = QuickAction(label=label, text=t))
                })
            )

        val response = client.post("https://api.line.me/v2/bot/message/reply") {
            header(HttpHeaders.Authorization, "Bearer $channelAccessToken")
            contentType(ContentType.Application.Json)
            setBody(ReplyBody(replyToken, listOf(msg)))
        }
        // ステータスコードが200系以外なら、LINE APIエラーとして例外を投げる
        if (!response.status.isSuccess()) {
            val body = response.body<String>()
            error("LINE reply failed: ${response.status} $body")
        }
    }
}