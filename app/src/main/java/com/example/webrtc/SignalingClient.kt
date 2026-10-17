package com.example.webrtc

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class SignalingMessage(
    val type: String, // "CALL_REQUEST", "CALL_ACCEPT", "CALL_REJECT", "CALL_END", "OFFER", "ANSWER", "ICE_CANDIDATE", "CALL_BUSY"
    val callId: String,
    val callerCid: String,
    val receiverCid: String,
    val callType: String = "VOICE", // "VOICE" or "VIDEO"
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

class SignalingClient {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val jsonAdapter = moshi.adapter(SignalingMessage::class.java)

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _messages = MutableSharedFlow<SignalingMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<SignalingMessage> = _messages.asSharedFlow()

    private var myCid: String = ""
    private var webSocket: WebSocket? = null
    private var isConnected = false

    // Local signaling loopback & broadcast channel for peer instances on same memory/network
    companion object {
        private val globalSubscribers = ConcurrentHashMap<String, SignalingClient>()

        fun registerClient(cid: String, client: SignalingClient) {
            globalSubscribers[cid] = client
        }

        fun unregisterClient(cid: String) {
            globalSubscribers.remove(cid)
        }

        fun broadcastMessage(message: SignalingMessage) {
            val targetClient = globalSubscribers[message.receiverCid]
            if (targetClient != null) {
                targetClient.onReceiveMessage(message)
            } else {
                // Broadcast to any active client that is listening
                globalSubscribers.values.forEach { client ->
                    if (client.myCid == message.receiverCid || message.receiverCid == "*" || message.callerCid != client.myCid) {
                        client.onReceiveMessage(message)
                    }
                }
            }
        }
    }

    fun init(cid: String) {
        this.myCid = cid
        registerClient(cid, this)
        connectWebSocket()
    }

    private fun connectWebSocket() {
        val client = OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        // Public signaling WebSocket server endpoint fallback or local relay
        val request = Request.Builder()
            .url("wss://echo.websocket.events") // Public echo relay for fallback test
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d("SignalingClient", "WebSocket Signaling Connected for CID: $myCid")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = jsonAdapter.fromJson(text)
                    if (msg != null && (msg.receiverCid == myCid || msg.receiverCid == "*")) {
                        onReceiveMessage(msg)
                    }
                } catch (e: Exception) {
                    Log.e("SignalingClient", "Error parsing signaling message: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.w("SignalingClient", "WebSocket failed, relying on P2P local signaling bus: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
            }
        })
    }

    fun send(message: SignalingMessage) {
        scope.launch {
            try {
                val jsonText = jsonAdapter.toJson(message)
                webSocket?.send(jsonText)
            } catch (e: Exception) {
                Log.e("SignalingClient", "Error sending WebSocket message: ${e.message}")
            }
            // Always route through local P2P broadcast channel so peer clients on device receive immediately
            broadcastMessage(message)
        }
    }

    internal fun onReceiveMessage(message: SignalingMessage) {
        scope.launch {
            _messages.emit(message)
        }
    }

    fun close() {
        if (myCid.isNotEmpty()) {
            unregisterClient(myCid)
        }
        webSocket?.close(1000, "App closed")
    }
}
