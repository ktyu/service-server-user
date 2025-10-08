package com.service.api.util

import java.security.MessageDigest

object Sha256HashingUtil {

    enum class SaltMode { PREFIX, SUFFIX, BOTH }

    /** SHA-256 해시를 16진 문자열(길이 64)로 반환. salt는 선택적. */
    fun sha256Hex(input: String, salt: String? = null, mode: SaltMode = SaltMode.PREFIX): String {
        val data = when {
            salt.isNullOrEmpty() -> input
            mode == SaltMode.PREFIX -> salt + input
            mode == SaltMode.SUFFIX -> input + salt
            else -> salt + input + salt
        }.toByteArray(Charsets.UTF_8)

        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { "%02x".format(it) } // VARCHAR(64)에 저장하기 적합
    }

    fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hash hex length must be even" }
        val size = hex.length / 2
        val result = ByteArray(size)
        var i = 0
        while (i < size) {
            val index = i * 2
            result[i] = hex.substring(index, index + 2).toInt(16).toByte()
            i++
        }
        return result
    }
}
