package dev.wristflick.wear

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import dev.wristflick.common.Command
import dev.wristflick.common.Protocol

class WatchActivity : Activity(), MessageClient.OnMessageReceivedListener {
    private lateinit var pad: GesturePad
    private val main = Handler(Looper.getMainLooper())
    private var resumed = false
    private var requestInFlight: Protocol.Request? = null
    private var targetNode: String? = null
    private var lastCommandAt = 0L
    private val timeout = Runnable {
        if (requestInFlight != null) {
            clearPending()
            show("No reply · check phone", false)
        }
    }
    private val poll = object : Runnable {
        override fun run() {
            if (resumed) {
                if (requestInFlight == null && SystemClock.elapsedRealtime() - lastCommandAt > 4500)
                    send(Command.STATUS)
                main.postDelayed(this, 6000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pad = GesturePad(this) { send(it) }
        setContentView(pad)
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        Wearable.getMessageClient(this).addListener(this)
            .addOnSuccessListener { if (resumed) send(Command.STATUS) }
            .addOnFailureListener { show("Google Play services unavailable", false) }
        main.postDelayed(poll, 6000)
    }

    override fun onPause() {
        resumed = false
        main.removeCallbacksAndMessages(null)
        clearPending()
        Wearable.getMessageClient(this).removeListener(this)
        super.onPause()
    }

    private fun send(command: Command) {
        if (!resumed) return
        if (requestInFlight != null) {
            // Status checks must not swallow the user's first swipe.
            if (requestInFlight?.command == Command.STATUS && command != Command.STATUS) clearPending()
            else return
        }
        val start = SystemClock.elapsedRealtime()
        if (command != Command.STATUS && start - lastCommandAt < 450) return
        if (command != Command.STATUS) lastCommandAt = start
        val request = Protocol.request(command)
        requestInFlight = request
        main.postDelayed(timeout, 4000)
        if (command != Command.STATUS) show("Sending…", false)

        Wearable.getCapabilityClient(this)
            .getCapability(Protocol.PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { capability ->
                if (!resumed || requestInFlight?.id != request.id) return@addOnSuccessListener
                val nearby = capability.nodes.filter { it.isNearby }
                if (nearby.size != 1) {
                    clearPending()
                    show("Connect phone · install companion", false)
                    return@addOnSuccessListener
                }
                if (SystemClock.elapsedRealtime() - start > 2500) {
                    clearPending(); show("Connection too slow · try again", false)
                    return@addOnSuccessListener
                }
                val node = nearby.single().id
                targetNode = node
                Wearable.getMessageClient(this)
                    .sendMessage(node, Protocol.COMMAND_PATH, request.bytes())
                    .addOnFailureListener {
                        if (requestInFlight?.id == request.id) {
                            clearPending(); show("Phone disconnected", false)
                        }
                    }
            }.addOnFailureListener {
                if (requestInFlight?.id == request.id) {
                    clearPending(); show("Cannot reach phone", false)
                }
            }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != Protocol.RESULT_PATH) return
        val result = Protocol.parseResult(event.data) ?: return
        main.post {
            val request = requestInFlight ?: return@post
            if (!resumed || event.sourceNodeId != targetNode || result.id != request.id) return@post
            clearPending()
            show(result.message, result.ok)
            if (result.ok && request.command != Command.STATUS)
                pad.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    private fun clearPending() {
        main.removeCallbacks(timeout)
        requestInFlight = null
        targetNode = null
    }
    private fun show(message: String, ok: Boolean) {
        if (!resumed) return
        pad.status = message
        pad.statusOk = ok
        pad.contentDescription = "TikTok remote. Swipe up previous, swipe down next, double-tap like. $message"
    }
}
