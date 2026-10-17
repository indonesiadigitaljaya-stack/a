package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CidDarkBackground
import com.example.ui.theme.CidPrimaryBlue
import com.example.webrtc.CallType
import com.example.webrtc.ConnectionState
import com.example.webrtc.WebRTCManager
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun WebRTCVideoView(
    videoTrack: VideoTrack?,
    eglBaseContext: EglBase.Context,
    isMirror: Boolean = false,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(eglBaseContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setMirror(isMirror)
                setEnableHardwareScaler(true)
            }
        },
        update = { renderer ->
            videoTrack?.addSink(renderer)
        },
        onRelease = { renderer ->
            videoTrack?.removeSink(renderer)
            renderer.release()
        },
        modifier = modifier
    )
}

@Composable
fun WebRTCCallScreen(
    webrtcManager: WebRTCManager,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    val callState by webrtcManager.callState.collectAsStateWithLifecycle()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission || !hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS
                )
            )
        }
    }

    val durationText = remember(callState.durationSeconds) {
        val minutes = callState.durationSeconds / 60
        val seconds = callState.durationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CidDarkBackground)
    ) {
        // INCOMING CALL VIEW
        if (callState.isIncoming && callState.connectionState == ConnectionState.RINGING) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(CidPrimaryBlue)
                        .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(70.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = callState.peerName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = callState.peerCid,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CidPrimaryBlue
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (callState.callType == CallType.VIDEO) "Panggilan Video Masuk..." else "Panggilan Suara Masuk...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(60.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reject Button
                    FloatingActionButton(
                        onClick = { webrtcManager.rejectCall() },
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Tolak Panggilan",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Accept Button
                    FloatingActionButton(
                        onClick = { webrtcManager.answerCall() },
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Terima Panggilan",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        } else {
            // ACTIVE / CONNECTING CALL VIEW
            val activeVideoTrack = webrtcManager.remoteVideoTrack ?: webrtcManager.localVideoTrack
            if (callState.callType == CallType.VIDEO && callState.isCameraOn && activeVideoTrack != null) {
                // Render Active Camera Video Track on Canvas
                WebRTCVideoView(
                    videoTrack = activeVideoTrack,
                    eglBaseContext = webrtcManager.rootEglBase.eglBaseContext,
                    isMirror = activeVideoTrack == webrtcManager.localVideoTrack && callState.isFrontCamera,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Audio or Pre-video Avatar Center View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(40.dp))

                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(CidPrimaryBlue)
                            .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(70.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = callState.peerName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = callState.peerCid,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CidPrimaryBlue
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Connection Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when (callState.connectionState) {
                            ConnectionState.P2P_DIRECT_CONNECTED -> Color(0xFF10B981).copy(alpha = 0.2f)
                            ConnectionState.TURN_FALLBACK_CONNECTED -> Color(0xFFFB8500).copy(alpha = 0.2f)
                            else -> CidPrimaryBlue.copy(alpha = 0.2f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (callState.connectionState) {
                                    ConnectionState.INITIATING_P2P -> "Menghubungkan WebRTC P2P..."
                                    ConnectionState.P2P_DIRECT_CONNECTED -> "WebRTC P2P Direct (${callState.latencyMs}ms)"
                                    ConnectionState.TURN_FALLBACK_CONNECTED -> "TURN Relayed Fallback (${callState.latencyMs}ms)"
                                    ConnectionState.REJECTED -> "Panggilan Ditolak"
                                    ConnectionState.BUSY -> "Pengguna Sibuk"
                                    ConnectionState.TIMEOUT -> "Tidak Ada Jawaban"
                                    else -> "Menghubungkan..."
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (callState.connectionState) {
                            ConnectionState.INITIATING_P2P -> "Memanggil..."
                            ConnectionState.P2P_DIRECT_CONNECTED, ConnectionState.TURN_FALLBACK_CONNECTED -> durationText
                            else -> callState.errorMessage ?: "Menghubungkan..."
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    // Live Audio Sound Waveform Indicator during active voice calls
                    if (callState.callType == CallType.VOICE &&
                        (callState.connectionState == ConnectionState.P2P_DIRECT_CONNECTED ||
                         callState.connectionState == ConnectionState.TURN_FALLBACK_CONNECTED)
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(7) { index ->
                                val heightMultiplier = remember(callState.durationSeconds, index) {
                                    if (callState.isMuted) 8.dp else ((index % 3 + 1) * 12 + (callState.durationSeconds % 4) * 5).dp
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp, heightMultiplier)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (callState.isMuted) Color.Gray else CidPrimaryBlue)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Real Local Camera Picture-in-Picture Preview (Top-Right)
            if (callState.callType == CallType.VIDEO && callState.isCameraOn && webrtcManager.localVideoTrack != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 40.dp, end = 20.dp)
                        .size(110.dp, 160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.DarkGray)
                        .border(2.dp, CidPrimaryBlue, RoundedCornerShape(16.dp))
                        .align(Alignment.TopEnd)
                ) {
                    WebRTCVideoView(
                        videoTrack = webrtcManager.localVideoTrack,
                        eglBaseContext = webrtcManager.rootEglBase.eglBaseContext,
                        isMirror = callState.isFrontCamera,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom Call Controls Bar
            Surface(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color(0xFF16213E),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Fallback TURN indicator trigger
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { webrtcManager.simulateTurnFallback() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SyncProblem, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Uji Fallback TURN Relay", fontSize = 11.sp, color = Color.LightGray)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute Audio Button
                        IconButton(
                            onClick = { webrtcManager.toggleMute() },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (callState.isMuted) Color.White else Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                tint = if (callState.isMuted) Color.Black else Color.White
                            )
                        }

                        // Speaker Toggle Button
                        IconButton(
                            onClick = { webrtcManager.toggleSpeaker() },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (callState.isSpeakerOn) CidPrimaryBlue else Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speaker",
                                tint = Color.White
                            )
                        }

                        // Camera Controls (if Video Call)
                        if (callState.callType == CallType.VIDEO) {
                            IconButton(
                                onClick = { webrtcManager.toggleCamera() },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (callState.isCameraOn) CidPrimaryBlue else Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = if (callState.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = "Camera",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { webrtcManager.switchCamera() },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Switch Camera",
                                    tint = Color.White
                                )
                            }
                        }

                        // End Call FAB
                        FloatingActionButton(
                            onClick = onEndCall,
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Akhiri Panggilan",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
