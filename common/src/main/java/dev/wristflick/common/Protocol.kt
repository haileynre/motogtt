package dev.wristflick.common

import org.json.JSONObject
import java.util.UUID

object Protocol {
    const val COMMAND_PATH = "/wristflick/v1/command"
    const val RESULT_PATH = "/wristflick/v1/result"
    const val PHONE_CAPABILITY = "wristflick_phone_v1"
    const val MAX_AGE_MS = 10_000L

    data class Request(val id: String, val command: Command, val sentAt: Long) {
        fun bytes(): ByteArray = JSONObject().put("version", 1).put("id", id)
            .put("command", command.name).put("sentAt", sentAt)
            .toString().toByteArray(Charsets.UTF_8)
    }

    data class Result(val id: String, val ok: Boolean, val message: String) {
        fun bytes(): ByteArray = JSONObject().put("version", 1).put("id", id)
            .put("ok", ok).put("message", message)
            .toString().toByteArray(Charsets.UTF_8)
    }

    fun request(command: Command): Request =
        Request(UUID.randomUUID().toString(), command, System.currentTimeMillis())

    fun parseRequest(bytes: ByteArray): Request? = runCatching {
        require(bytes.size in 1..512)
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        require(json.getInt("version") == 1)
        val id = json.getString("id")
        require(UUID.fromString(id).toString() == id)
        Request(id, Command.valueOf(json.getString("command")), json.getLong("sentAt"))
    }.getOrNull()

    fun parseResult(bytes: ByteArray): Result? = runCatching {
        require(bytes.size in 1..1024)
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        require(json.getInt("version") == 1)
        Result(json.getString("id"), json.getBoolean("ok"), json.getString("message").take(120))
    }.getOrNull()

    fun isFresh(sentAt: Long, now: Long): Boolean =
        sentAt >= now - MAX_AGE_MS && sentAt <= now + MAX_AGE_MS
}
