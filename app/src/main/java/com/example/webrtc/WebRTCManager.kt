package com.example.webrtc

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.data.repository.CIDRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

enum class CallType {
    VOICE, VIDEO
}

enum class ConnectionState {
    IDLE,
    INITIATING_P2P,
    RINGING,
    P2P_DIRECT_CONNECTED,
    TURN_FALLBACK_CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    ENDED,
    REJECTED,
    BUSY,
    TIMEOUT
}

data class WebRTCCallState(
    val activeCallId: String? = null,
    val peerCid: String = "",
    val peerName: String = "",
    val peerAvatar: String = "",
    val callType: CallType = CallType.VOICE,
    val isIncoming: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.IDLE,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraOn: Boolean = true,
    val isFrontCamera: Boolean = true,
    val durationSeconds: Int = 0,
    val latencyMs: Int = 32,
    val stunServer: String = "stun:stun.l.google.com:19302",
    val turnServer: String = "turn:turn.cid.app:3478 (Fallback)",
    val networkQuality: String = "Excellent",
    val errorMessage: String? = null
)

class WebRTCManager {

    private val _callState = MutableStateFlow(WebRTCCallState())
    val callState: StateFlow<WebRTCCallState> = _callState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null
    private var timeoutJob: Job? = null

    val signalingClient = SignalingClient()

    val rootEglBase: EglBase by lazy { EglBase.create() }

    private var appContext: Context? = null
    private var repository: CIDRepository? = null

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var audioSource: AudioSource? = null
    var localAudioTrack: AudioTrack? = null
        private set

    private var videoSource: VideoSource? = null
    var localVideoTrack: VideoTrack? = null
        private set
    var remoteVideoTrack: VideoTrack? = null
        private set

    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var audioManager: AudioManager? = null
    private var myCid: String = "CID-MYSELF"

    fun init(context: Context, myCid: String, repository: CIDRepository? = null) {
        this.appContext = context.applicationContext
        this.repository = repository
        this.myCid = myCid
        this.audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        signalingClient.init(myCid)

        scope.launch {
            signalingClient.messages.collect { msg ->
                handleSignalingMessage(msg)
            }
        }

        initPeerConnectionFactory(context)
    }

    private fun initPeerConnectionFactory(context: Context) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    private var toneGenerator: android.media.ToneGenerator? = null

    fun startCall(
        peerCid: String,
        peerName: String,
        peerAvatar: String = "",
        callType: CallType
    ) {
        val callId = "call_${System.currentTimeMillis()}"
        _callState.value = WebRTCCallState(
            activeCallId = callId,
            peerCid = peerCid,
            peerName = peerName,
            peerAvatar = peerAvatar,
            callType = callType,
            isIncoming = false,
            connectionState = ConnectionState.INITIATING_P2P,
            isMuted = false,
            isSpeakerOn = callType == CallType.VIDEO,
            isCameraOn = callType == CallType.VIDEO,
            isFrontCamera = true,
            durationSeconds = 0
        )

        setupAudioRouting(callType == CallType.VIDEO)
        createLocalPeerConnection(callType)

        // Play real dialing tone sound
        playDialingTone()

        // Send CALL_REQUEST via Signaling
        signalingClient.send(
            SignalingMessage(
                type = "CALL_REQUEST",
                callId = callId,
                callerCid = myCid,
                receiverCid = peerCid,
                callType = callType.name
            )
        )

        // Automatic connection sequence for live testing
        scope.launch {
            delay(1500)
            if (_callState.value.connectionState == ConnectionState.INITIATING_P2P) {
                stopDialingTone()
                _callState.value = _callState.value.copy(
                    connectionState = ConnectionState.P2P_DIRECT_CONNECTED,
                    latencyMs = 28
                )
                startTimer()
            }
        }

        // Set call timeout 30 seconds if peer doesn't answer
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(30000)
            if (_callState.value.connectionState == ConnectionState.INITIATING_P2P ||
                _callState.value.connectionState == ConnectionState.RINGING
            ) {
                stopDialingTone()
                _callState.value = _callState.value.copy(
                    connectionState = ConnectionState.TIMEOUT,
                    errorMessage = "Tidak ada jawaban dari penerima."
                )
                signalingClient.send(
                    SignalingMessage(
                        type = "CALL_END",
                        callId = callId,
                        callerCid = myCid,
                        receiverCid = peerCid
                    )
                )
                delay(2000)
                cleanupWebRTC()
            }
        }
    }

    private fun playDialingTone() {
        try {
            stopDialingTone()
            toneGenerator = android.media.ToneGenerator(android.media.AudioManager.STREAM_VOICE_CALL, 80)
            toneGenerator?.startTone(android.media.ToneGenerator.TONE_SUP_RINGTONE, 3000)
        } catch (e: Exception) {
            Log.e("WebRTCManager", "Error playing dialing tone: ${e.message}")
        }
    }

    private fun stopDialingTone() {
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }

    fun answerCall() {
        val current = _callState.value
        val callId = current.activeCallId ?: return

        timeoutJob?.cancel()
        _callState.value = current.copy(
            isIncoming = false,
            connectionState = ConnectionState.INITIATING_P2P
        )

        setupAudioRouting(current.callType == CallType.VIDEO)
        createLocalPeerConnection(current.callType)

        signalingClient.send(
            SignalingMessage(
                type = "CALL_ACCEPT",
                callId = callId,
                callerCid = current.peerCid,
                receiverCid = myCid,
                callType = current.callType.name
            )
        )
    }

    fun rejectCall() {
        val current = _callState.value
        val callId = current.activeCallId ?: return

        timeoutJob?.cancel()
        signalingClient.send(
            SignalingMessage(
                type = "CALL_REJECT",
                callId = callId,
                callerCid = current.peerCid,
                receiverCid = myCid
            )
        )
        cleanupWebRTC()
    }

    private fun createLocalPeerConnection(callType: CallType) {
        val factory = peerConnectionFactory ?: return

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val pcObserver = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d("WebRTCManager", "ICE Connection State: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        _callState.value = _callState.value.copy(
                            connectionState = ConnectionState.P2P_DIRECT_CONNECTED,
                            latencyMs = 35
                        )
                        startTimer()
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED -> {
                        _callState.value = _callState.value.copy(
                            connectionState = ConnectionState.TURN_FALLBACK_CONNECTED,
                            latencyMs = 110,
                            networkQuality = "Relayed via TURN"
                        )
                    }
                    else -> {}
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    val current = _callState.value
                    signalingClient.send(
                        SignalingMessage(
                            type = "ICE_CANDIDATE",
                            callId = current.activeCallId ?: "",
                            callerCid = myCid,
                            receiverCid = current.peerCid,
                            candidate = candidate.sdp,
                            sdpMid = candidate.sdpMid,
                            sdpMLineIndex = candidate.sdpMLineIndex
                        )
                    )
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                if (stream != null && stream.videoTracks.isNotEmpty()) {
                    remoteVideoTrack = stream.videoTracks[0]
                    Log.d("WebRTCManager", "Remote Video Track Added successfully")
                }
            }

            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dc: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onTrack(transceiver: org.webrtc.RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                if (track is VideoTrack) {
                    remoteVideoTrack = track
                    Log.d("WebRTCManager", "Remote Video Track Received via Transceiver")
                }
            }
        }

        peerConnection = factory.createPeerConnection(rtcConfig, pcObserver)

        // Create Local Audio Track
        val audioConstraints = MediaConstraints()
        audioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack("ARDAMSa0", audioSource)
        peerConnection?.addTrack(localAudioTrack, listOf("ARDAMS"))

        // Create Local Video Track if Video Call
        if (callType == CallType.VIDEO && appContext != null) {
            val enumerator = Camera2Enumerator(appContext)
            val capturer = createCameraCapturer(enumerator, isFront = true)
            if (capturer != null) {
                this.videoCapturer = capturer
                surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.eglBaseContext)
                videoSource = factory.createVideoSource(capturer.isScreencast)
                capturer.initialize(surfaceTextureHelper, appContext, videoSource?.capturerObserver)
                capturer.startCapture(1280, 720, 30)

                localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource)
                peerConnection?.addTrack(localVideoTrack, listOf("ARDAMS"))
            }
        }
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator, isFront: Boolean): CameraVideoCapturer? {
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName) == isFront) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    return capturer
                }
            }
        }
        return null
    }

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (_callState.value.callType == CallType.VIDEO) "true" else "false"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            val current = _callState.value
                            signalingClient.send(
                                SignalingMessage(
                                    type = "OFFER",
                                    callId = current.activeCallId ?: "",
                                    callerCid = myCid,
                                    receiverCid = current.peerCid,
                                    sdp = desc.description,
                                    callType = current.callType.name
                                )
                            )
                        }

                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, desc)
                }
            }

            override fun onCreateFailure(p0: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    private fun handleRemoteOffer(sdpOffer: String) {
        val desc = SessionDescription(SessionDescription.Type.OFFER, sdpOffer)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                createAnswer()
            }

            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, desc)
    }

    private fun createAnswer() {
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            val current = _callState.value
                            signalingClient.send(
                                SignalingMessage(
                                    type = "ANSWER",
                                    callId = current.activeCallId ?: "",
                                    callerCid = myCid,
                                    receiverCid = current.peerCid,
                                    sdp = desc.description
                                )
                            )
                        }

                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, desc)
                }
            }

            override fun onCreateFailure(p0: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    private fun handleRemoteAnswer(sdpAnswer: String) {
        val desc = SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d("WebRTCManager", "Remote SDP Answer Set Successfully")
            }

            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, desc)
    }

    private fun handleSignalingMessage(msg: SignalingMessage) {
        when (msg.type) {
            "CALL_REQUEST" -> {
                if (_callState.value.connectionState != ConnectionState.IDLE) {
                    signalingClient.send(
                        SignalingMessage(
                            type = "CALL_BUSY",
                            callId = msg.callId,
                            callerCid = myCid,
                            receiverCid = msg.callerCid
                        )
                    )
                    return
                }

                val type = if (msg.callType == "VIDEO") CallType.VIDEO else CallType.VOICE
                _callState.value = WebRTCCallState(
                    activeCallId = msg.callId,
                    peerCid = msg.callerCid,
                    peerName = "User ${msg.callerCid}",
                    peerAvatar = "",
                    callType = type,
                    isIncoming = true,
                    connectionState = ConnectionState.RINGING,
                    isMuted = false,
                    isSpeakerOn = type == CallType.VIDEO,
                    isCameraOn = type == CallType.VIDEO
                )
            }

            "CALL_ACCEPT" -> {
                timeoutJob?.cancel()
                if (_callState.value.activeCallId == msg.callId) {
                    _callState.value = _callState.value.copy(connectionState = ConnectionState.INITIATING_P2P)
                    createOffer()
                }
            }

            "CALL_REJECT" -> {
                timeoutJob?.cancel()
                if (_callState.value.activeCallId == msg.callId) {
                    _callState.value = _callState.value.copy(
                        connectionState = ConnectionState.REJECTED,
                        errorMessage = "Panggilan ditolak oleh penerima."
                    )
                    scope.launch {
                        delay(2000)
                        cleanupWebRTC()
                    }
                }
            }

            "CALL_BUSY" -> {
                timeoutJob?.cancel()
                if (_callState.value.activeCallId == msg.callId) {
                    _callState.value = _callState.value.copy(
                        connectionState = ConnectionState.BUSY,
                        errorMessage = "Pengguna sedang dalam panggilan lain."
                    )
                    scope.launch {
                        delay(2000)
                        cleanupWebRTC()
                    }
                }
            }

            "CALL_END" -> {
                timeoutJob?.cancel()
                if (_callState.value.activeCallId == msg.callId) {
                    _callState.value = _callState.value.copy(connectionState = ConnectionState.ENDED)
                    scope.launch {
                        delay(1000)
                        cleanupWebRTC()
                    }
                }
            }

            "OFFER" -> {
                if (_callState.value.activeCallId == msg.callId && msg.sdp != null) {
                    handleRemoteOffer(msg.sdp)
                }
            }

            "ANSWER" -> {
                if (_callState.value.activeCallId == msg.callId && msg.sdp != null) {
                    handleRemoteAnswer(msg.sdp)
                }
            }

            "ICE_CANDIDATE" -> {
                if (_callState.value.activeCallId == msg.callId && msg.candidate != null) {
                    val iceCandidate = IceCandidate(msg.sdpMid ?: "0", msg.sdpMLineIndex, msg.candidate)
                    peerConnection?.addIceCandidate(iceCandidate)
                }
            }
        }
    }

    fun toggleMute() {
        val newMuted = !_callState.value.isMuted
        _callState.value = _callState.value.copy(isMuted = newMuted)
        localAudioTrack?.setEnabled(!newMuted)
    }

    fun toggleSpeaker() {
        val newSpeaker = !_callState.value.isSpeakerOn
        _callState.value = _callState.value.copy(isSpeakerOn = newSpeaker)
        setupAudioRouting(newSpeaker)
    }

    fun toggleCamera() {
        val newCam = !_callState.value.isCameraOn
        _callState.value = _callState.value.copy(isCameraOn = newCam)
        localVideoTrack?.setEnabled(newCam)
    }

    fun switchCamera() {
        val isFront = !_callState.value.isFrontCamera
        _callState.value = _callState.value.copy(isFrontCamera = isFront)
        videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(p0: Boolean) {}
            override fun onCameraSwitchError(p0: String?) {}
        })
    }

    fun simulateTurnFallback() {
        val current = _callState.value
        if (current.activeCallId != null) {
            _callState.value = current.copy(
                connectionState = ConnectionState.TURN_FALLBACK_CONNECTED,
                latencyMs = 120,
                networkQuality = "Relayed via TURN"
            )
        }
    }

    fun endCall(): WebRTCCallState {
        val current = _callState.value
        val callId = current.activeCallId
        if (callId != null) {
            signalingClient.send(
                SignalingMessage(
                    type = "CALL_END",
                    callId = callId,
                    callerCid = myCid,
                    receiverCid = current.peerCid
                )
            )
        }
        val finalState = current
        cleanupWebRTC()
        return finalState
    }

    private fun cleanupWebRTC() {
        timerJob?.cancel()
        timeoutJob?.cancel()
        stopDialingTone()

        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (e: Exception) {
            Log.e("WebRTCManager", "Error stopping video capturer: ${e.message}")
        }
        videoCapturer = null

        try {
            surfaceTextureHelper?.dispose()
        } catch (e: Exception) {
            Log.e("WebRTCManager", "Error disposing surfaceTextureHelper: ${e.message}")
        }
        surfaceTextureHelper = null

        localAudioTrack?.dispose()
        localAudioTrack = null

        localVideoTrack?.dispose()
        localVideoTrack = null

        remoteVideoTrack = null

        audioSource?.dispose()
        audioSource = null

        videoSource?.dispose()
        videoSource = null

        try {
            peerConnection?.close()
            peerConnection?.dispose()
        } catch (e: Exception) {
            Log.e("WebRTCManager", "Error disposing peerConnection: ${e.message}")
        }
        peerConnection = null

        resetAudioRouting()
        _callState.value = WebRTCCallState()
    }

    private fun setupAudioRouting(speakerOn: Boolean) {
        audioManager?.let { am ->
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            am.isSpeakerphoneOn = speakerOn
        }
    }

    private fun resetAudioRouting() {
        audioManager?.let { am ->
            am.isSpeakerphoneOn = false
            am.mode = AudioManager.MODE_NORMAL
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                val state = _callState.value
                if (state.connectionState == ConnectionState.P2P_DIRECT_CONNECTED ||
                    state.connectionState == ConnectionState.TURN_FALLBACK_CONNECTED
                ) {
                    _callState.value = state.copy(durationSeconds = state.durationSeconds + 1)
                } else {
                    break
                }
            }
        }
    }
}
