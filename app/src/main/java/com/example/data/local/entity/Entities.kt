package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val cid: String, // e.g., CID-58392174
    val username: String,
    val phone: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val bio: String = "Connecting via CID",
    val status: String = "Online",
    val isCurrentUser: Boolean = false,
    val isLoggedIn: Boolean = true,
    val loginMethod: String = "GOOGLE", // "GOOGLE" or "PHONE"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val cid: String, // Unique identifier CID-XXXXXXXX
    val username: String,
    val avatarUrl: String = "",
    val bio: String = "",
    val isFavorite: Boolean = false,
    val status: String = "ACCEPTED" // "ACCEPTED", "PENDING", "BLOCKED"
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String, // e.g. peer CID
    val senderCid: String,
    val receiverCid: String,
    val text: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null, // "IMAGE", "VOICE", "FILE", "LOCATION"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT", // "SENDING", "SENT", "DELIVERED", "READ", "FAILED"
    val isLocalOnly: Boolean = false
)

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey val callId: String,
    val peerCid: String,
    val peerName: String,
    val peerAvatar: String = "",
    val type: String, // "VOICE", "VIDEO"
    val direction: String, // "INCOMING", "OUTGOING", "MISSED"
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val transactionId: String,
    val type: String, // "TOP_UP", "TRANSFER_SENT", "TRANSFER_RECEIVED", "MERCHANT_PAY"
    val amount: Long,
    val peerCidOrName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS",
    val note: String = ""
)
