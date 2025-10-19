package com.example.interfaceadapters

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class LineSignatureVerifier(private val channelSecret: String) {

    fun isValid(signatureBase64: String?, body: ByteArray): Boolean {

        if (signatureBase64.isNullOrBlank()) return false

        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(channelSecret.toByteArray(), "HmacSHA256"))
        }

        val digest = mac.doFinal(body)
        val expected = Base64.getEncoder().encodeToString(digest)
        // 時間一定比較
        return MessageDigest.isEqual(
            expected.toByteArray(), signatureBase64.toByteArray()
        )
    }
}
