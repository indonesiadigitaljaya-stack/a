package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.repository.CIDRepository
import com.example.ui.dialogs.PrivacySettingsDialog
import com.example.ui.dialogs.ScanQRDialog
import com.example.ui.dialogs.SearchCIDDialog
import com.example.ui.dialogs.TopUpPayDialog
import com.example.ui.dialogs.TransferPayDialog
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.CIDPayScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MyCIDProfileScreen
import com.example.ui.screens.WebRTCCallScreen
import com.example.ui.theme.CIDTheme
import com.example.ui.theme.CidPayGold
import com.example.ui.theme.CidPrimaryBlue
import com.example.webrtc.CallType
import com.example.webrtc.WebRTCManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CIDApp() {
    val context = LocalContext.current
    val repository = remember { CIDRepository(context) }
    val webrtcManager = remember { WebRTCManager() }

    val currentUser by repository.getCurrentUserFlow().collectAsStateWithLifecycle(initialValue = null)
    val callState by webrtcManager.callState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(currentUser?.cid) {
        val cid = currentUser?.cid ?: "CID-100001"
        webrtcManager.init(context, cid, repository)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chat, 1: Calls, 2: Contacts, 3: CID Pay, 4: My CID Profile
    var activeChatPeerCid by remember { mutableStateOf<String?>(null) }

    // Dialog Visibility States
    var showSearchCidDialog by remember { mutableStateOf(false) }
    var showScanQrDialog by remember { mutableStateOf(false) }
    var showTopUpDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var transferTargetCid by remember { mutableStateOf("") }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) } // Default false -> Clean White Background Theme

    val scope = rememberCoroutineScope()

    CIDTheme(darkTheme = isDarkTheme) {
        if (currentUser?.isLoggedIn == false) {
            LoginScreen(
                repository = repository,
                onLoginSuccess = {
                    selectedTab = 0
                }
            )
        } else if (callState.activeCallId != null) {
            WebRTCCallScreen(
                webrtcManager = webrtcManager,
                onEndCall = {
                    val endedCall = webrtcManager.endCall()
                    scope.launch {
                        repository.logCall(
                            peerCid = endedCall.peerCid,
                            peerName = endedCall.peerName,
                            peerAvatar = endedCall.peerAvatar,
                            type = if (endedCall.callType == CallType.VOICE) "VOICE" else "VIDEO",
                            direction = if (endedCall.isIncoming) "INCOMING" else "OUTGOING",
                            durationSeconds = endedCall.durationSeconds
                        )
                    }
                }
            )
        } else if (activeChatPeerCid != null) {
            // Active Chat Detail Screen
            ChatDetailScreen(
                peerCid = activeChatPeerCid!!,
                repository = repository,
                onBack = { activeChatPeerCid = null },
                onStartCall = { cid, name, type ->
                    webrtcManager.startCall(cid, name, "", type)
                }
            )
        } else {
            // Main Container with Bottom Navigation
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // CID Logo Shield Icon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CidPrimaryBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "CID Shield",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "CID",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = CidPrimaryBlue
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CidPrimaryBlue.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = currentUser?.cid ?: "CID-58392174",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CidPrimaryBlue,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Connect • Communicate • Pay",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSearchCidDialog = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search CID")
                            }
                            IconButton(onClick = { showScanQrDialog = true }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                            }
                            IconButton(onClick = { selectedTab = 4 }) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(CidPrimaryBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "My CID Profile",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                            label = { Text("Chat") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CidPrimaryBlue,
                                selectedTextColor = CidPrimaryBlue,
                                indicatorColor = CidPrimaryBlue.copy(alpha = 0.15f)
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
                            label = { Text("Calls") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CidPrimaryBlue,
                                selectedTextColor = CidPrimaryBlue,
                                indicatorColor = CidPrimaryBlue.copy(alpha = 0.15f)
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.People, contentDescription = "Contacts") },
                            label = { Text("Contacts") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CidPrimaryBlue,
                                selectedTextColor = CidPrimaryBlue,
                                indicatorColor = CidPrimaryBlue.copy(alpha = 0.15f)
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Pay") },
                            label = { Text("CID Pay") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CidPayGold,
                                selectedTextColor = CidPayGold,
                                indicatorColor = CidPayGold.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        0 -> ChatListScreen(
                            repository = repository,
                            onOpenChat = { peerCid -> activeChatPeerCid = peerCid },
                            onNewChatClick = { showSearchCidDialog = true }
                        )
                        1 -> CallsScreen(
                            repository = repository,
                            onStartCall = { cid, name, type ->
                                webrtcManager.startCall(cid, name, "", type)
                            },
                            onNewCallClick = { showSearchCidDialog = true }
                        )
                        2 -> ContactsScreen(
                            repository = repository,
                            onOpenChat = { peerCid -> activeChatPeerCid = peerCid },
                            onStartCall = { cid, name, type ->
                                webrtcManager.startCall(cid, name, "", type)
                            },
                            onTransferPay = { targetCid ->
                                transferTargetCid = targetCid
                                showTransferDialog = true
                            },
                            onSearchCidClick = { showSearchCidDialog = true },
                            onScanQrClick = { showScanQrDialog = true }
                        )
                        3 -> CIDPayScreen(
                            repository = repository,
                            onTopUpClick = { showTopUpDialog = true },
                            onTransferClick = {
                                transferTargetCid = ""
                                showTransferDialog = true
                            },
                            onScanQrClick = { showScanQrDialog = true }
                        )
                        4 -> MyCIDProfileScreen(
                            repository = repository,
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = { isDarkTheme = it },
                            onOpenPrivacySettings = { showPrivacyDialog = true },
                            onLogout = { selectedTab = 0 }
                        )
                    }
                }
            }
        }

        // Dialog Overlays
        if (showSearchCidDialog) {
            SearchCIDDialog(
                repository = repository,
                onDismiss = { showSearchCidDialog = false },
                onOpenChat = { peerCid ->
                    showSearchCidDialog = false
                    activeChatPeerCid = peerCid
                }
            )
        }

        if (showScanQrDialog) {
            ScanQRDialog(
                onDismiss = { showScanQrDialog = false },
                onScannedCid = { scannedCid ->
                    showScanQrDialog = false
                    activeChatPeerCid = scannedCid
                }
            )
        }

        if (showTopUpDialog) {
            TopUpPayDialog(
                repository = repository,
                onDismiss = { showTopUpDialog = false }
            )
        }

        if (showTransferDialog) {
            TransferPayDialog(
                repository = repository,
                initialTargetCid = transferTargetCid,
                onDismiss = { showTransferDialog = false }
            )
        }

        if (showPrivacyDialog) {
            PrivacySettingsDialog(
                onDismiss = { showPrivacyDialog = false }
            )
        }
    }
}
