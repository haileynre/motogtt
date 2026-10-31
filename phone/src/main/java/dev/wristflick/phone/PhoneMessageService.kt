package dev.wristflick.phone

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dev.wristflick.common.Protocol

class PhoneMessageService : WearableListenerService() {
    private val main = Handler(Looper.getMainLooper())
    // One process-wide table across service rebindings; all access occurs on the main thread.
    companion object { private val recent = LinkedHashSet<String>() }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != Protocol.COMMAND_PATH) return
        val request = Protocol.parseRequest(event.data) ?: return
        val receivedAt = SystemClock.elapsedRealtime()
        val source = event.sourceNodeId
        // No cloud-origin control and no broadcast: accept only a currently nearby node.
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            main.post {
                if (nodes.none { it.id == source && it.isNearby }) {
                    reply(source, request.id, false, "Keep watch connected nearby")
                    return@post
                }
                if (recent.contains(request.id)) return@post
                recent.add(request.id)
                while (recent.size > 128) recent.remove(recent.first())
                if (SystemClock.elapsedRealtime() - receivedAt > 2000 ||
                    !Protocol.isFresh(request.sentAt, System.currentTimeMillis())) {
                    reply(source, request.id, false, "Request expired; check device clocks")
                    return@post
                }
                val controller = TikTokAccessibilityService.instance
                if (controller == null) {
                    reply(source, request.id, false, "Enable accessibility on phone")
                    return@post
                }
                controller.execute(request.command) { ok, message ->
                    reply(source, request.id, ok, message)
                }
            }
        }.addOnFailureListener {
            // Fail closed. Sending no command is safer than retrying an unknown result.
            main.post { reply(source, request.id, false, "Phone connection unavailable") }
        }
    }

    private fun reply(node: String, id: String, ok: Boolean, message: String) {
        RemoteState.report(message)
        Wearable.getMessageClient(this).sendMessage(node, Protocol.RESULT_PATH,
            Protocol.Result(id, ok, message).bytes())
    }
}
