package com.example.data.repository

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class CodeSent(val verificationId: String, val token: PhoneAuthProvider.ForceResendingToken) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthRepository(
    private val context: Context? = null
) {

    private val auth: FirebaseAuth?
        get() {
            return try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                context?.let { ctx ->
                    try {
                        if (com.google.firebase.FirebaseApp.getApps(ctx).isEmpty()) {
                            val options = com.google.firebase.FirebaseOptions.Builder()
                                .setApplicationId("1:1070286926896:android:b8971f49615a995e")
                                .setApiKey("AIzaSyBPlaceholderKeyForCIDApplet00000")
                                .setProjectId("cid-network-app")
                                .setGcmSenderId("1070286926896")
                                .build()
                            com.google.firebase.FirebaseApp.initializeApp(ctx, options)
                        }
                    } catch (_: Exception) {}
                }
                try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
            }
        }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    // 1. Firebase Email Registration
    suspend fun registerWithEmail(email: String, password: String): Result<FirebaseUser?> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth belum dikonfigurasi"))
        _authState.value = AuthState.Loading
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).awaitTask()
            val user = result.user
            _authState.value = AuthState.Success(user)
            Result.success(user)
        } catch (e: Exception) {
            val errorMessage = parseAuthError(e)
            _authState.value = AuthState.Error(errorMessage)
            Result.failure(Exception(errorMessage, e))
        }
    }

    private fun parseAuthError(e: Exception): String {
        val msg = e.localizedMessage ?: e.message ?: ""
        return when {
            msg.contains("API key not valid", ignoreCase = true) ->
                "API Key Firebase belum dikonfigurasi / tidak valid. Masukkan FIREBASE_API_KEY yang valid dari Firebase Console di panel Secrets AI Studio atau google-services.json."
            msg.contains("Play Services unavailable", ignoreCase = true) ->
                "Google Play Services tidak tersedia pada perangkat ini."
            msg.contains("network error", ignoreCase = true) ->
                "Koneksi jaringan terputus. Silakan periksa koneksi internet Anda."
            else -> msg.ifBlank { "Terjadi kesalahan pada layanan autentikasi" }
        }
    }

    // 2. Firebase Email Sign In
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser?> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth belum dikonfigurasi"))
        _authState.value = AuthState.Loading
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).awaitTask()
            val user = result.user
            _authState.value = AuthState.Success(user)
            Result.success(user)
        } catch (e: Exception) {
            val errorMessage = parseAuthError(e)
            _authState.value = AuthState.Error(errorMessage)
            Result.failure(Exception(errorMessage, e))
        }
    }

    // 3. Google Sign-In with Credential Manager
    suspend fun signInWithGoogle(context: Context, webClientId: String? = null): Result<FirebaseUser?> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth belum dikonfigurasi"))
        _authState.value = AuthState.Loading
        return try {
            val credentialManager = CredentialManager.create(context)
            val serverClientId = webClientId ?: "1070286926896-placeholder.apps.googleusercontent.com"

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).awaitTask()
                val user = authResult.user
                _authState.value = AuthState.Success(user)
                Result.success(user)
            } else {
                val err = "Tipe kredensial Google tidak valid"
                _authState.value = AuthState.Error(err)
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            val errorMessage = parseAuthError(e)
            _authState.value = AuthState.Error(errorMessage)
            Result.failure(Exception(errorMessage, e))
        }
    }

    // 4. Phone Authentication - Request Code
    fun sendPhoneVerificationCode(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onError: (Exception) -> Unit,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit = {}
    ) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            val err = Exception("Firebase Auth belum dikonfigurasi")
            _authState.value = AuthState.Error(err.localizedMessage ?: "")
            onError(err)
            return
        }
        _authState.value = AuthState.Loading

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onVerificationCompleted(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                val errorMessage = parseAuthError(e)
                _authState.value = AuthState.Error(errorMessage)
                onError(Exception(errorMessage, e))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                _authState.value = AuthState.CodeSent(verificationId, token)
                onCodeSent(verificationId, token)
            }
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // 4b. Phone Authentication - Verify Code
    suspend fun signInWithPhoneCode(verificationId: String, code: String): Result<FirebaseUser?> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth belum dikonfigurasi"))
        _authState.value = AuthState.Loading
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val result = firebaseAuth.signInWithCredential(credential).awaitTask()
            val user = result.user
            _authState.value = AuthState.Success(user)
            Result.success(user)
        } catch (e: Exception) {
            val errorMessage = parseAuthError(e)
            _authState.value = AuthState.Error(errorMessage)
            Result.failure(Exception(errorMessage, e))
        }
    }

    // Sign out
    fun signOut() {
        auth?.signOut()
        _authState.value = AuthState.Idle
    }
}

// Helper extension to await Firebase Tasks cleanly without extra dependency conflicts
private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            cont.resume(result)
        }
        addOnFailureListener { exception ->
            cont.resumeWithException(exception)
        }
        addOnCanceledListener {
            cont.cancel()
        }
    }
