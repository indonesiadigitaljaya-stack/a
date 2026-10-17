package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.repository.CIDRepository
import com.example.ui.theme.CidPayGold
import com.example.ui.theme.CidPrimaryBlue
import com.example.ui.theme.CidSuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCIDProfileScreen(
    repository: CIDRepository,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    onOpenPrivacySettings: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val currentUser by repository.getCurrentUserFlow().collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isEditing by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var bioInput by remember { mutableStateOf("") }
    var statusInput by remember { mutableStateOf("") }
    var showAvatarPickerModal by remember { mutableStateOf(false) }

    // Image Picker Launcher for selecting personal photo from device gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                repository.updateProfileAvatar(it.toString())
                Toast.makeText(context, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Interactive Settings States
    var notificationsEnabled by remember { mutableStateOf(true) }
    var callSoundEnabled by remember { mutableStateOf(true) }
    var vibrateEnabled by remember { mutableStateOf(true) }
    var biometricLockEnabled by remember { mutableStateOf(false) }
    var p2pEncryptionEnabled by remember { mutableStateOf(true) }
    var cacheSizeMb by remember { mutableStateOf("14.2 MB") }

    val cid = currentUser?.cid ?: "CID-58392174"
    val avatarUrl = currentUser?.avatarUrl.orEmpty()
    val loginMethod = currentUser?.loginMethod ?: "GOOGLE"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Avatar Frame with Photo Edit Overlay
        Box(
            modifier = Modifier
                .size(105.dp)
                .clickable { showAvatarPickerModal = true },
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(CidPrimaryBlue)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Foto Profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            // Camera Badge Icon to trigger photo change
            Surface(
                shape = CircleShape,
                color = CidPrimaryBlue,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(32.dp)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Ubah Foto",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Ubah Foto Profil Action Text
        TextButton(onClick = { showAvatarPickerModal = true }) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp), tint = CidPrimaryBlue)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Ubah Foto Profil Saya", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CidPrimaryBlue)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = currentUser?.username ?: "Budi Santoso",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Large CID Identity Badge & Login Method Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CidPrimaryBlue.copy(alpha = 0.12f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CidPrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cid,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CidPrimaryBlue
                    )
                }
            }

            // Active Login Method Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (loginMethod == "GOOGLE") Color(0xFFEA4335).copy(alpha = 0.12f) else CidSuccessGreen.copy(alpha = 0.12f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (loginMethod == "GOOGLE") Icons.Default.Email else Icons.Default.Phone,
                        contentDescription = null,
                        tint = if (loginMethod == "GOOGLE") Color(0xFFEA4335) else CidSuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (loginMethod == "GOOGLE") "Google" else "No. HP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (loginMethod == "GOOGLE") Color(0xFFEA4335) else CidSuccessGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ------------------ SECTION 1: DETAIL PROFIL ------------------
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CidPrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detail Profil Saya", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    IconButton(onClick = {
                        if (isEditing) {
                            scope.launch {
                                repository.updateProfile(
                                    username = usernameInput.ifBlank { currentUser?.username ?: "" },
                                    bio = bioInput,
                                    status = statusInput
                                )
                                isEditing = false
                                Toast.makeText(context, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            usernameInput = currentUser?.username ?: ""
                            bioInput = currentUser?.bio ?: ""
                            statusInput = currentUser?.status ?: ""
                            isEditing = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Edit Profil",
                            tint = CidPrimaryBlue
                        )
                    }
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("Bio / Catatan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = statusInput,
                        onValueChange = { statusInput = it },
                        label = { Text("Status Kehadiran") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Bio", fontSize = 11.sp, color = Color.Gray)
                    Text(currentUser?.bio ?: "Connecting securely with CID P2P Network", fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Status", fontSize = 11.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CidSuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(currentUser?.status ?: "Online • P2P Active", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(if (loginMethod == "GOOGLE") "Email Terhubung" else "Nomor HP Terhubung", fontSize = 11.sp, color = Color.Gray)
                    Text(if (loginMethod == "GOOGLE") (currentUser?.email ?: "budi@gmail.com") else (currentUser?.phone ?: "+62 812 3456 7890"), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ------------------ SECTION 2: QR CODE IDENTITAS ------------------
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = CidPrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "QR CODE IDENTITAS CID",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CIDQRCanvas(cid = cid)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Gunakan QR Identitas CID ini untuk terhubung secara P2P aman tanpa membagikan nomor telepon.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("CID", cid)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "CID $cid berhasil disalin!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CidPrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin CID", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Membagikan CID $cid...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = CidPrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ------------------ SECTION 3: PENGATURAN TEMA & TAMPILAN ------------------
        SettingsGroupCard(title = "Pengaturan Tema & Tampilan", icon = Icons.Default.Palette) {
            // White Theme Switch
            SettingsSwitchRow(
                title = "Latar Tema Putih (White Theme)",
                subtitle = if (!isDarkTheme) "Aktif: Clean White Theme (#FFFFFF)" else "Nonaktif: Dark Theme",
                icon = Icons.Default.Palette,
                checked = !isDarkTheme,
                onCheckedChange = { isWhiteTheme ->
                    onToggleDarkTheme(!isWhiteTheme)
                    Toast.makeText(
                        context,
                        if (isWhiteTheme) "Tema Putih diaktifkan" else "Tema Gelap diaktifkan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Dark Mode Direct Toggle
            SettingsSwitchRow(
                title = "Mode Gelap (Dark Mode)",
                subtitle = "Ganti tampilan latar belakang menjadi serba gelap",
                icon = Icons.Default.DarkMode,
                checked = isDarkTheme,
                onCheckedChange = { dark ->
                    onToggleDarkTheme(dark)
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ------------------ SECTION 4: PRIVASI & KEAMANAN ------------------
        SettingsGroupCard(title = "Privasi & Keamanan", icon = Icons.Default.Shield) {
            SettingsClickableRow(
                title = "Visibilitas CID & Izin Kontak",
                subtitle = "Atur siapa yang dapat mencari dan mengirim pesan",
                icon = Icons.Default.PrivacyTip,
                onClick = onOpenPrivacySettings
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingsSwitchRow(
                title = "Kunci Biometrik / PIN Aplikasi",
                subtitle = "Minta autentikasi sidik jari saat membuka CID",
                icon = Icons.Default.Lock,
                checked = biometricLockEnabled,
                onCheckedChange = {
                    biometricLockEnabled = it
                    Toast.makeText(context, if (it) "Kunci Biometrik Diaktifkan" else "Kunci Biometrik Dinonaktifkan", Toast.LENGTH_SHORT).show()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingsSwitchRow(
                title = "Enkripsi P2P End-to-End",
                subtitle = "Enkripsi kunci publik otomatis untuk setiap pesan",
                icon = Icons.Default.Security,
                checked = p2pEncryptionEnabled,
                onCheckedChange = {
                    p2pEncryptionEnabled = it
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ------------------ SECTION 5: NOTIFIKASI & DERING ------------------
        SettingsGroupCard(title = "Notifikasi & Dering", icon = Icons.Default.Notifications) {
            SettingsSwitchRow(
                title = "Notifikasi Pesan Masuk",
                subtitle = "Tampilkan notifikasi saat pesan P2P baru diterima",
                icon = Icons.Default.Notifications,
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingsSwitchRow(
                title = "Suara Panggilan WebRTC",
                subtitle = "Mainkan nada dering saat ada panggilan masuk",
                icon = Icons.Default.VolumeUp,
                checked = callSoundEnabled,
                onCheckedChange = { callSoundEnabled = it }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ------------------ SECTION 6: AKUN & KELUAR ------------------
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = Color(0xFFEA4335),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pengaturan Sesi & Akun", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Anda terhubung via ${if (loginMethod == "GOOGLE") "Google Account" else "Nomor HP"}. Anda dapat keluar untuk berganti metode masuk.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repository.logout()
                            Toast.makeText(context, "Sesi berhasil diakhiri.", Toast.LENGTH_SHORT).show()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEA4335)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar / Ganti Akun Login", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ------------------ AVATAR PICKER DIALOG ------------------
    if (showAvatarPickerModal) {
        AlertDialog(
            onDismissRequest = { showAvatarPickerModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CidPrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ubah Foto Profil", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pilih foto pribadi dari galeri perangkat Anda atau gunakan sampel avatar:", fontSize = 13.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 1: Gallery Picker Button
                    Button(
                        onClick = {
                            showAvatarPickerModal = false
                            galleryLauncher.launch("image/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CidPrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih Foto dari Galeri HP", fontWeight = FontWeight.Bold)
                    }

                    // Option 2: Preset Personal Photo Avatars
                    Text("Atau pilih Foto Sampel:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    val sampleAvatars = listOf(
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        sampleAvatars.forEachIndexed { index, url ->
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, CidPrimaryBlue, CircleShape)
                                    .clickable {
                                        scope.launch {
                                            repository.updateProfileAvatar(url)
                                            showAvatarPickerModal = false
                                            Toast.makeText(context, "Foto profil berhasil diubah!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Avatar $index",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    if (avatarUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repository.updateProfileAvatar("")
                                    showAvatarPickerModal = false
                                    Toast.makeText(context, "Foto profil dihapus.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEA4335)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hapus Foto Profil Saat Ini", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarPickerModal = false }) {
                    Text("Tutup", color = CidPrimaryBlue)
                }
            }
        )
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CidPrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Column {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CidPrimaryBlue
            )
        )
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = CidPrimaryBlue,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun CIDQRCanvas(cid: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val size = this.size.minDimension
        val gridSize = 10
        val cellSize = size / gridSize

        val darkColor = Color.Black

        // Corner 1
        drawRect(darkColor, Offset(0f, 0f), Size(cellSize * 3, cellSize * 3))
        drawRect(Color.White, Offset(cellSize, cellSize), Size(cellSize, cellSize))

        // Corner 2
        drawRect(darkColor, Offset(size - cellSize * 3, 0f), Size(cellSize * 3, cellSize * 3))
        drawRect(Color.White, Offset(size - cellSize * 2, cellSize), Size(cellSize, cellSize))

        // Corner 3
        drawRect(darkColor, Offset(0f, size - cellSize * 3), Size(cellSize * 3, cellSize * 3))
        drawRect(Color.White, Offset(cellSize, size - cellSize * 2), Size(cellSize, cellSize))

        // Data pattern grid derived from CID hash
        val hash = cid.hashCode()
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                if ((i < 3 && j < 3) || (i > 6 && j < 3) || (i < 3 && j > 6)) continue
                val fill = ((hash shr (i + j)) and 1) == 1
                if (fill) {
                    drawRect(
                        color = darkColor,
                        topLeft = Offset(i * cellSize, j * cellSize),
                        size = Size(cellSize * 0.9f, cellSize * 0.9f)
                    )
                }
            }
        }
    }
}
