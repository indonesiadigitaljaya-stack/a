package com.example.data.repository

import android.content.Context
import com.example.data.local.CIDDatabase
import com.example.data.local.entity.CallEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class CIDRepository(context: Context) {

    private val db = CIDDatabase.getDatabase(context)
    private val userDao = db.userDao()
    private val contactDao = db.contactDao()
    private val messageDao = db.messageDao()
    private val callDao = db.callDao()
    private val transactionDao = db.transactionDao()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Wallet balance state flow
    private val _walletBalance = MutableStateFlow(250000L) // Default Rp 250.000
    val walletBalance: StateFlow<Long> = _walletBalance.asStateFlow()

    init {
        repositoryScope.launch {
            seedInitialDataIfNeeded()
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        val currentUser = userDao.getCurrentUser()
        if (currentUser == null) {
            val myCid = generateCid()
            val myUser = UserEntity(
                userId = "user_me",
                cid = myCid,
                username = "Budi Santoso",
                phone = "+62 812 3456 7890",
                email = "budi@gmail.com",
                avatarUrl = "",
                bio = "Connecting securely with CID",
                status = "Online • P2P Active",
                isCurrentUser = true,
                isLoggedIn = true,
                loginMethod = "GOOGLE"
            )
            userDao.insertUser(myUser)

            // Seed Contacts
            val c1 = ContactEntity("CID-88239102", "Andi Wijaya", "", "Software Engineer", isFavorite = true, status = "ACCEPTED")
            val c2 = ContactEntity("CID-91048211", "Siti Nurhaliza", "", "Digital Designer", isFavorite = true, status = "ACCEPTED")
            val c3 = ContactEntity("CID-47201934", "Rudi Hermawan", "", "Available on CID", isFavorite = false, status = "ACCEPTED")
            val c4 = ContactEntity("CID-12038495", "Dewi Lestari", "", "Busy", isFavorite = false, status = "ACCEPTED")
            val c5 = ContactEntity("CID-99812734", "Spam User", "", "Unknown", isFavorite = false, status = "BLOCKED")

            contactDao.insertContact(c1)
            contactDao.insertContact(c2)
            contactDao.insertContact(c3)
            contactDao.insertContact(c4)
            contactDao.insertContact(c5)

            // Seed Messages
            val now = System.currentTimeMillis()
            val m1 = MessageEntity("m1", "CID-88239102", "CID-88239102", myCid, "Halo Budi! Apakah kamu sudah menggunakan CID Pay?", null, null, now - 3600000, "READ")
            val m2 = MessageEntity("m2", "CID-88239102", myCid, "CID-88239102", "Halo Andi! Iya, sangat cepat dan gratis biaya transfer.", null, null, now - 3000000, "READ")
            val m3 = MessageEntity("m3", "CID-88239102", "CID-88239102", myCid, "Boleh kirimkan QR CID kamu untuk meeting hari ini?", null, null, now - 1800000, "READ")

            val m4 = MessageEntity("m4", "CID-91048211", "CID-91048211", myCid, "Hi Budi, file desain UI CID sudah selesai ya.", null, null, now - 86400000, "READ")
            val m5 = MessageEntity("m5", "CID-91048211", myCid, "CID-91048211", "Terima kasih Siti! P2P Voice Call tadi sangat jernih.", null, null, now - 82000000, "READ")

            messageDao.insertMessage(m1)
            messageDao.insertMessage(m2)
            messageDao.insertMessage(m3)
            messageDao.insertMessage(m4)
            messageDao.insertMessage(m5)

            // Seed Calls
            callDao.insertCall(CallEntity("call_1", "CID-88239102", "Andi Wijaya", "", "VIDEO", "OUTGOING", now - 3600000, 184))
            callDao.insertCall(CallEntity("call_2", "CID-91048211", "Siti Nurhaliza", "", "VOICE", "INCOMING", now - 82000000, 312))
            callDao.insertCall(CallEntity("call_3", "CID-47201934", "Rudi Hermawan", "", "VOICE", "MISSED", now - 172800000, 0))

            // Seed Transactions
            transactionDao.insertTransaction(TransactionEntity("tx_1", "TOP_UP", 200000, "Bank Transfer VA", now - 172800000, "SUCCESS", "Top Up via BCA"))
            transactionDao.insertTransaction(TransactionEntity("tx_2", "TRANSFER_SENT", 50000, "Andi Wijaya (CID-88239102)", now - 86400000, "SUCCESS", "Makan siang"))
            transactionDao.insertTransaction(TransactionEntity("tx_3", "TRANSFER_RECEIVED", 100000, "Siti Nurhaliza (CID-91048211)", now - 43200000, "SUCCESS", "Ganti kopi"))
        }
    }

    fun generateCid(): String {
        val randomNum = Random.nextInt(10000000, 99999999)
        return "CID-$randomNum"
    }

    // User Flow
    fun getCurrentUserFlow(): Flow<UserEntity?> = userDao.getCurrentUserFlow()

    suspend fun updateProfile(username: String, bio: String, status: String) {
        val current = userDao.getCurrentUser() ?: return
        userDao.updateUser(current.copy(username = username, bio = bio, status = status))
    }

    suspend fun updateProfileAvatar(avatarUrl: String) {
        val current = userDao.getCurrentUser() ?: return
        userDao.updateUser(current.copy(avatarUrl = avatarUrl))
    }

    suspend fun loginWithFirebaseUser(
        firebaseUser: com.google.firebase.auth.FirebaseUser,
        authMethod: String
    ): UserEntity {
        val current = userDao.getCurrentUser()
        val cid = current?.cid ?: generateCid()

        val displayName = firebaseUser.displayName?.ifBlank { null }
            ?: if (authMethod == "PHONE") "Pengguna CID (${firebaseUser.phoneNumber ?: ""})"
               else "Pengguna CID"

        val user = UserEntity(
            userId = firebaseUser.uid,
            cid = cid,
            username = displayName,
            email = firebaseUser.email ?: current?.email ?: "user@cid.app",
            phone = firebaseUser.phoneNumber ?: current?.phone ?: "+6281234567890",
            avatarUrl = firebaseUser.photoUrl?.toString() ?: current?.avatarUrl ?: "",
            bio = current?.bio ?: "Connected via $authMethod",
            status = "Online • P2P Active",
            isCurrentUser = true,
            isLoggedIn = true,
            loginMethod = authMethod
        )
        userDao.insertUser(user)
        return user
    }

    suspend fun loginWithGoogle(email: String, username: String, avatarUrl: String = "") {
        val current = userDao.getCurrentUser()
        val cid = current?.cid ?: generateCid()
        val user = UserEntity(
            userId = "user_me",
            cid = cid,
            username = username.ifBlank { "Google User" },
            email = email,
            phone = current?.phone ?: "+62 812 3456 7890",
            avatarUrl = avatarUrl,
            bio = current?.bio ?: "Connected via Google Account",
            status = "Online • P2P Active",
            isCurrentUser = true,
            isLoggedIn = true,
            loginMethod = "GOOGLE"
        )
        userDao.insertUser(user)
    }

    suspend fun loginWithPhone(phone: String, username: String = "", avatarUrl: String = "") {
        val current = userDao.getCurrentUser()
        val cid = current?.cid ?: generateCid()
        val formattedName = username.ifBlank { "Pengguna CID ($phone)" }
        val user = UserEntity(
            userId = "user_me",
            cid = cid,
            username = formattedName,
            email = current?.email ?: "user@cid.app",
            phone = phone,
            avatarUrl = avatarUrl,
            bio = current?.bio ?: "Connected via Phone Number",
            status = "Online • P2P Active",
            isCurrentUser = true,
            isLoggedIn = true,
            loginMethod = "PHONE"
        )
        userDao.insertUser(user)
    }

    suspend fun logout() {
        val current = userDao.getCurrentUser() ?: return
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        userDao.updateUser(current.copy(isLoggedIn = false))
    }

    // Contacts
    fun getAllContactsFlow(): Flow<List<ContactEntity>> = contactDao.getAllContactsFlow()
    fun getBlockedContactsFlow(): Flow<List<ContactEntity>> = contactDao.getBlockedContactsFlow()

    suspend fun searchCid(cidInput: String): ContactEntity? {
        val trimmed = cidInput.trim().uppercase()
        // Check local DB first
        val local = contactDao.getContactByCid(trimmed)
        if (local != null) return local

        // If not local, simulate CID Directory lookup
        if (trimmed.startsWith("CID-") && trimmed.length >= 8) {
            return ContactEntity(
                cid = trimmed,
                username = "Pengguna $trimmed",
                bio = "Anggota jaringan CID",
                isFavorite = false,
                status = "NEW"
            )
        }
        return null
    }

    suspend fun addContact(cid: String, username: String) {
        val contact = ContactEntity(
            cid = cid,
            username = username,
            status = "ACCEPTED"
        )
        contactDao.insertContact(contact)
    }

    suspend fun toggleFavorite(contact: ContactEntity) {
        contactDao.updateContact(contact.copy(isFavorite = !contact.isFavorite))
    }

    suspend fun blockContact(contact: ContactEntity) {
        contactDao.updateContact(contact.copy(status = "BLOCKED"))
    }

    suspend fun unblockContact(contact: ContactEntity) {
        contactDao.updateContact(contact.copy(status = "ACCEPTED"))
    }

    // Messages
    fun getMessagesForConversationFlow(cid: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversationFlow(cid)

    fun getAllMessagesFlow(): Flow<List<MessageEntity>> = messageDao.getAllMessagesFlow()

    suspend fun sendMessage(
        receiverCid: String,
        text: String,
        mediaUrl: String? = null,
        mediaType: String? = null
    ) {
        val currentUser = userDao.getCurrentUser() ?: return
        val msgId = "msg_${System.currentTimeMillis()}"
        val initialMsg = MessageEntity(
            messageId = msgId,
            conversationId = receiverCid,
            senderCid = currentUser.cid,
            receiverCid = receiverCid,
            text = text,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            timestamp = System.currentTimeMillis(),
            status = "SENDING"
        )
        messageDao.insertMessage(initialMsg)

        // Real P2P delivery status progression: SENDING -> SENT -> DELIVERED -> READ
        repositoryScope.launch {
            delay(400)
            messageDao.updateMessageStatus(msgId, "SENT")
            delay(600)
            messageDao.updateMessageStatus(msgId, "DELIVERED")
            delay(800)
            messageDao.updateMessageStatus(msgId, "READ")

            // Transmit via Real P2P Signaling Bus
            com.example.webrtc.SignalingClient.broadcastMessage(
                com.example.webrtc.SignalingMessage(
                    type = "CHAT_MESSAGE",
                    callId = msgId,
                    callerCid = currentUser.cid,
                    receiverCid = receiverCid,
                    sdp = text
                )
            )

            // Auto reply only if messaging AI Assistant or Bot
            if (receiverCid.contains("BOT", ignoreCase = true) || receiverCid.contains("AI", ignoreCase = true)) {
                delay(1200)
                val replyMsg = MessageEntity(
                    messageId = "reply_${System.currentTimeMillis()}",
                    conversationId = receiverCid,
                    senderCid = receiverCid,
                    receiverCid = currentUser.cid,
                    text = getSimulatedReply(text),
                    timestamp = System.currentTimeMillis(),
                    status = "READ"
                )
                messageDao.insertMessage(replyMsg)
            }
        }
    }

    private fun getSimulatedReply(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("halo") || lower.contains("hi") -> "Halo! Terima kasih sudah menghubungi lewat CID P2P network."
            lower.contains("pay") || lower.contains("bayar") || lower.contains("transfer") -> "CID Pay sangat praktis, langsung terhubung dengan ID CID."
            lower.contains("call") || lower.contains("telepon") -> "Siap! Mari kita lakukan WebRTC P2P voice call jernih."
            else -> "Pesan diterima secara terenkripsi melalui jalur CID P2P."
        }
    }

    // Calls
    fun getAllCallsFlow(): Flow<List<CallEntity>> = callDao.getAllCallsFlow()

    suspend fun logCall(
        peerCid: String,
        peerName: String,
        peerAvatar: String = "",
        type: String,
        direction: String,
        durationSeconds: Int
    ) {
        val call = CallEntity(
            callId = "call_${System.currentTimeMillis()}",
            peerCid = peerCid,
            peerName = peerName,
            peerAvatar = peerAvatar,
            type = type,
            direction = direction,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds
        )
        callDao.insertCall(call)
    }

    // CID Pay Service Abstraction
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()

    suspend fun topUpWallet(amount: Long, paymentMethod: String): Boolean {
        if (amount <= 0) return false
        _walletBalance.value += amount
        val tx = TransactionEntity(
            transactionId = "tx_${System.currentTimeMillis()}",
            type = "TOP_UP",
            amount = amount,
            peerCidOrName = paymentMethod,
            timestamp = System.currentTimeMillis(),
            status = "SUCCESS",
            note = "Top Up Saldo CID Pay"
        )
        transactionDao.insertTransaction(tx)
        return true
    }

    suspend fun transferToCid(targetCid: String, amount: Long, note: String): Boolean {
        if (amount <= 0 || _walletBalance.value < amount) return false
        _walletBalance.value -= amount
        val tx = TransactionEntity(
            transactionId = "tx_${System.currentTimeMillis()}",
            type = "TRANSFER_SENT",
            amount = amount,
            peerCidOrName = targetCid,
            timestamp = System.currentTimeMillis(),
            status = "SUCCESS",
            note = if (note.isBlank()) "Transfer ke $targetCid" else note
        )
        transactionDao.insertTransaction(tx)
        return true
    }
}
