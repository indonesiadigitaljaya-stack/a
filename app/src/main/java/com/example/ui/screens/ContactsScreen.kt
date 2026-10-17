package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ContactEntity
import com.example.data.repository.CIDRepository
import com.example.ui.theme.CidPayGold
import com.example.ui.theme.CidPrimaryBlue
import com.example.webrtc.CallType
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(
    repository: CIDRepository,
    onOpenChat: (String) -> Unit,
    onStartCall: (String, String, CallType) -> Unit,
    onTransferPay: (String) -> Unit,
    onSearchCidClick: () -> Unit,
    onScanQrClick: () -> Unit
) {
    val contacts by repository.getAllContactsFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val blockedContacts by repository.getBlockedContactsFlow().collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val filteredList = remember(contacts, blockedContacts, selectedTabIndex, searchQuery) {
        val baseList = when (selectedTabIndex) {
            0 -> contacts // All
            1 -> contacts.filter { it.isFavorite } // Favorites
            else -> blockedContacts // Blocked
        }
        if (searchQuery.isBlank()) baseList
        else baseList.filter {
            it.username.contains(searchQuery, ignoreCase = true) || it.cid.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSearchCidClick,
                containerColor = CidPrimaryBlue,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Contact")
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

            // Action Header Buttons: [Search CID] & [Scan QR CID]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSearchCidClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CidPrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cari CID")
                }

                Button(
                    onClick = onScanQrClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan QR")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Inline Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter kontak...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CidPrimaryBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs: All, Favorites, Blocked
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = CidPrimaryBlue
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Semua (${contacts.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Favorit", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Diblokir (${blockedContacts.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tidak Ada Kontak", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.cid }) { contact ->
                        ContactItemCard(
                            contact = contact,
                            onChatClick = { onOpenChat(contact.cid) },
                            onVoiceClick = { onStartCall(contact.cid, contact.username, CallType.VOICE) },
                            onVideoClick = { onStartCall(contact.cid, contact.username, CallType.VIDEO) },
                            onPayClick = { onTransferPay(contact.cid) },
                            onToggleFavorite = {
                                scope.launch { repository.toggleFavorite(contact) }
                            },
                            onBlockToggle = {
                                scope.launch {
                                    if (contact.status == "BLOCKED") {
                                        repository.unblockContact(contact)
                                        Toast.makeText(context, "Kontak dibuka kembali", Toast.LENGTH_SHORT).show()
                                    } else {
                                        repository.blockContact(contact)
                                        Toast.makeText(context, "Kontak diblokir", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItemCard(
    contact: ContactEntity,
    onChatClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onVideoClick: () -> Unit,
    onPayClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBlockToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CidPrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = contact.cid,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CidPrimaryBlue
                    )
                    if (contact.bio.isNotBlank()) {
                        Text(
                            text = contact.bio,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (contact.isFavorite) CidPayGold else Color.Gray
                    )
                }

                IconButton(onClick = onBlockToggle) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Block",
                        tint = if (contact.status == "BLOCKED") MaterialTheme.colorScheme.error else Color.Gray
                    )
                }
            }

            if (contact.status != "BLOCKED") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onChatClick) {
                        Icon(Icons.Default.Chat, contentDescription = "Chat", tint = CidPrimaryBlue)
                    }
                    IconButton(onClick = onVoiceClick) {
                        Icon(Icons.Default.Call, contentDescription = "Voice", tint = CidPrimaryBlue)
                    }
                    IconButton(onClick = onVideoClick) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video", tint = CidPrimaryBlue)
                    }
                    IconButton(onClick = onPayClick) {
                        Icon(Icons.Default.Payment, contentDescription = "Pay", tint = CidPayGold)
                    }
                }
            }
        }
    }
}
