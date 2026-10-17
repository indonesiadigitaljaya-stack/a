package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.MessageEntity
import com.example.data.repository.CIDRepository
import com.example.ui.theme.CidPrimaryBlue
import com.example.webrtc.CallType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    peerCid: String,
    repository: CIDRepository,
    onBack: () -> Unit,
    onStartCall: (String, String, CallType) -> Unit
) {
    val messages by repository.getMessagesForConversationFlow(peerCid).collectAsStateWithLifecycle(initialValue = emptyList())
    val contacts by repository.getAllContactsFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUser by repository.getCurrentUserFlow().collectAsStateWithLifecycle(initialValue = null)

    val contact = remember(contacts, peerCid) {
        contacts.find { it.cid == peerCid }
    }
    val peerName = contact?.username ?: peerCid

    var textInput by remember { mutableStateOf("") }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var voiceFilePath by remember { mutableStateOf<String?>(null) }

    var selectedFullImageUri by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                repository.sendMessage(
                    receiverCid = peerCid,
                    text = "📷 Foto dikirim",
                    mediaUrl = uri.toString(),
                    mediaType = "IMAGE"
                )
                Toast.makeText(context, "Foto P2P berhasil dikirim!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Audio Permission Launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecording(
                context = context,
                onStarted = { recorder, file ->
                    mediaRecorder = recorder
                    voiceFilePath = file.absolutePath
                    isRecordingVoice = true
                },
                onError = { err ->
                    Toast.makeText(context, "Gagal merekam suara: $err", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "Izin mikrofon diperlukan untuk merekam voice note", Toast.LENGTH_SHORT).show()
        }
    }

    // Voice recording timer loop
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingDuration = 0
            while (isRecordingVoice) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CidPrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = peerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$peerCid • P2P Direct",
                                fontSize = 11.sp,
                                color = CidPrimaryBlue
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onStartCall(peerCid, peerName, CallType.VOICE) }) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = CidPrimaryBlue)
                    }
                    IconButton(onClick = { onStartCall(peerCid, peerName, CallType.VIDEO) }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = CidPrimaryBlue)
                    }
                    IconButton(onClick = { Toast.makeText(context, "CID Security: End-to-End P2P Encrypted", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // P2P Security Header Notice
            Surface(
                color = CidPrimaryBlue.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔒 P2P Encrypted • Direct Connection Active",
                    fontSize = 11.sp,
                    color = CidPrimaryBlue,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.messageId }) { msg ->
                    val isMe = msg.senderCid == (currentUser?.cid ?: "")
                    MessageBubble(
                        message = msg,
                        isMe = isMe,
                        onImageClick = { url -> selectedFullImageUri = url }
                    )
                }
            }

            // Active Voice Recording Bar
            AnimatedVisibility(visible = isRecordingVoice) {
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Merekam Voice Note P2P... (${String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60)})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }

                        Row {
                            IconButton(onClick = {
                                stopVoiceRecording(mediaRecorder)
                                mediaRecorder = null
                                isRecordingVoice = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Batal", tint = Color.Gray)
                            }

                            IconButton(onClick = {
                                stopVoiceRecording(mediaRecorder)
                                mediaRecorder = null
                                isRecordingVoice = false
                                val path = voiceFilePath
                                if (path != null && File(path).exists()) {
                                    scope.launch {
                                        repository.sendMessage(
                                            receiverCid = peerCid,
                                            text = "🎤 Pesan Suara P2P (${String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60)})",
                                            mediaUrl = path,
                                            mediaType = "VOICE"
                                        )
                                    }
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim Voice Note", tint = CidPrimaryBlue)
                            }
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Add, contentDescription = "Lampirkan Foto", tint = CidPrimaryBlue)
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Ketik pesan P2P...", fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CidPrimaryBlue,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (textInput.isBlank()) {
                        IconButton(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    startVoiceRecording(
                                        context = context,
                                        onStarted = { recorder, file ->
                                            mediaRecorder = recorder
                                            voiceFilePath = file.absolutePath
                                            isRecordingVoice = true
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, "Gagal merekam: $err", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CidPrimaryBlue.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Note", tint = CidPrimaryBlue)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val msgText = textInput
                                textInput = ""
                                scope.launch {
                                    repository.sendMessage(peerCid, msgText)
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CidPrimaryBlue)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Full Screen Image Preview Dialog
    selectedFullImageUri?.let { imgUrl ->
        Dialog(onDismissRequest = { selectedFullImageUri = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { selectedFullImageUri = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imgUrl,
                    contentDescription = "Full Image Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

private fun startVoiceRecording(
    context: Context,
    onStarted: (MediaRecorder, File) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val audioFile = File(context.cacheDir, "p2p_voice_${System.currentTimeMillis()}.3gp")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder.setOutputFile(audioFile.absolutePath)
        recorder.prepare()
        recorder.start()
        onStarted(recorder, audioFile)
    } catch (e: Exception) {
        onError(e.message ?: "Error launching recorder")
    }
}

private fun stopVoiceRecording(recorder: MediaRecorder?) {
    try {
        recorder?.stop()
        recorder?.release()
    } catch (_: Exception) {}
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isMe: Boolean,
    onImageClick: (String) -> Unit
) {
    val dateString = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMe) CidPrimaryBlue else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Media Content Rendering (Image or Real Voice Note)
                when (message.mediaType) {
                    "IMAGE" -> {
                        val imageUrl = message.mediaUrl
                        if (!imageUrl.isNull_or_blank_safe()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Shared Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick(imageUrl!!) },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    "VOICE" -> {
                        val voicePath = message.mediaUrl
                        RealVoiceNotePlayer(voicePath = voicePath, isMe = isMe)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateString,
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        ChatMessageStatusIcon(status = message.status)
                    }
                }
            }
        }
    }
}

@Composable
fun RealVoiceNotePlayer(voicePath: String?, isMe: Boolean) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(voicePath) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isMe) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (voicePath.isNull_or_blank_safe()) {
                    Toast.makeText(context, "File rekaman tidak ditemukan", Toast.LENGTH_SHORT).show()
                    return@IconButton
                }
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    try {
                        if (mediaPlayer == null) {
                            val player = MediaPlayer().apply {
                                setDataSource(context, Uri.parse(voicePath))
                                prepare()
                                setOnCompletionListener {
                                    isPlaying = false
                                }
                            }
                            mediaPlayer = player
                        }
                        mediaPlayer?.start()
                        isPlaying = true
                    } catch (e: Exception) {
                        Toast.makeText(context, "Gagal memutar audio: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isMe) Color.White else CidPrimaryBlue)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = if (isMe) CidPrimaryBlue else Color.White
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "Pesan Suara P2P",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isPlaying) "Memutar audio..." else "Sentuh untuk memutar",
                fontSize = 10.sp,
                color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray
            )
        }
    }
}

@Composable
fun ChatMessageStatusIcon(status: String) {
    when (status) {
        "SENDING" -> Text("🕒", fontSize = 10.sp)
        "SENT" -> Icon(Icons.Default.Check, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
        "DELIVERED", "READ" -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(12.dp))
        else -> Text("✓", fontSize = 10.sp, color = Color.White)
    }
}

private fun String?.isNull_or_blank_safe(): Boolean = this == null || this.isBlank()

