package com.example.interfaceadapters.line

import com.example.application.dto.FlexReplyMessageDto
import com.example.application.dto.TextReplyMessageDto
import com.example.application.usecase.ReplyUseCase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import kotlinx.serialization.json.*

/**
 * Controller
 * LINEへフォーマットへ変換して送る。
 */
class LineWebhookController(
    private val verifier: LineSignatureVerifier,
    private val replyUseCase: ReplyUseCase,
    private val lineClient: LineApiClient
) {
    suspend fun post(call: ApplicationCall) {

        // JSON取得
        val bodyBytes = call.receiveChannel().toByteArray()
        val signature = call.request.headers["X-Line-Signature"]
        // 署名検証が不正の場合、401で返却
        if (!verifier.isValid(signature, bodyBytes)) {
            call.respond(HttpStatusCode.Unauthorized); return
        }

        val root = Json.parseToJsonElement(String(bodyBytes)).jsonObject
        val events = root["events"]?.jsonArray ?: emptyList()

        for (e in events) {
            val ev = e.jsonObject

            if (ev["type"]?.jsonPrimitive?.content != "message") continue

            val replyToken = ev["replyToken"]?.jsonPrimitive?.content ?: continue

            val msg = ev["message"]?.jsonObject ?: continue

            if (msg["type"]?.jsonPrimitive?.content != "text") continue

            val text = msg["text"]?.jsonPrimitive?.content ?: continue

            val source = ev["source"]?.jsonObject ?: continue
            val userId = source["userId"]?.jsonPrimitive?.content ?: continue

            val reply = replyUseCase.execute(userId, text)
            when (reply) {
                is TextReplyMessageDto -> {
                    lineClient.replyText(
                        replyToken = replyToken,
                        text = reply.text,
                        quick = reply.quickReplies
                    )
                }
                is FlexReplyMessageDto -> {
                    lineClient.replyFlex(
                        replyToken = replyToken,
                        altText = reply.altText,
                        contents = reply.contents
                    )
                }
            }
        }
        call.respond(HttpStatusCode.OK)
    }
}

/** ByteReadChannel -> ByteArray ヘルパ */
private suspend fun ByteReadChannel.toByteArray(): ByteArray {
    val bytes = mutableListOf<Byte>()
    while (!isClosedForRead) {
        val p = readRemaining(8192)
        while (!p.isEmpty) bytes += p.readByte()
    }
    return bytes.toByteArray()
}

