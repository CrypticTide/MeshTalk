package com.meshtalk.app

import org.json.JSONObject
import java.util.UUID

data class MeshMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 6
) {
    fun encode(): ByteArray = JSONObject().apply {
        put("id", id); put("senderId", senderId); put("senderName", senderName)
        put("body", body); put("timestamp", timestamp); put("ttl", ttl)
    }.toString().toByteArray(Charsets.UTF_8)

    fun nextHop() = copy(ttl = ttl - 1)

    companion object {
        fun decode(bytes: ByteArray): MeshMessage? = runCatching {
            val j = JSONObject(bytes.toString(Charsets.UTF_8))
            MeshMessage(j.getString("id"), j.getString("senderId"), j.getString("senderName"),
                j.getString("body"), j.getLong("timestamp"), j.getInt("ttl"))
        }.getOrNull()
    }
}
