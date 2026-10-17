package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CallEntity
import com.example.data.repository.CIDRepository
import com.example.ui.theme.CidPrimaryBlue
import com.example.webrtc.CallType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallsScreen(
    repository: CIDRepository,
    onStartCall: (String, String, CallType) -> Unit,
    onNewCallClick: () -> Unit
) {
    val calls by repository.getAllCallsFlow().collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewCallClick,
                containerColor = CidPrimaryBlue,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = "New Call")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // WebRTC Architecture Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CidPrimaryBlue.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CidPrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "WebRTC P2P Voice & Video",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CidPrimaryBlue
                        )
                        Text(
                            text = "Koneksi langsung antar perangkat • Low latency & zero server media bandwidth.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Riwayat Panggilan",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (calls.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Belum Ada Panggilan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(calls, key = { it.callId }) { call ->
                        CallItemRow(
                            call = call,
                            onVoiceClick = { onStartCall(call.peerCid, call.peerName, CallType.VOICE) },
                            onVideoClick = { onStartCall(call.peerCid, call.peerName, CallType.VIDEO) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallItemRow(
    call: CallEntity,
    onVoiceClick: () -> Unit,
    onVideoClick: () -> Unit
) {
    val dateString = remember(call.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        sdf.format(Date(call.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(CidPrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.peerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CallDirectionIcon(direction = call.direction)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${call.peerCid} • $dateString",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onVoiceClick) {
                    Icon(Icons.Default.Call, contentDescription = "Voice", tint = CidPrimaryBlue)
                }
                IconButton(onClick = onVideoClick) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video", tint = CidPrimaryBlue)
                }
            }
        }
    }
}

@Composable
fun CallDirectionIcon(direction: String) {
    when (direction) {
        "INCOMING" -> Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
        "OUTGOING" -> Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = null, tint = CidPrimaryBlue, modifier = Modifier.size(14.dp))
        "MISSED" -> Icon(Icons.AutoMirrored.Filled.CallMissed, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
        else -> Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
    }
}
