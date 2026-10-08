package com.example.data.service

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.model.UserProfile
import com.example.util.PreferencesManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

    private val prefs = PreferencesManager(context)
    private val credentialManager = CredentialManager.create(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentUser = MutableStateFlow<UserProfile?>(prefs.getUserProfile())
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val firebaseAuth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.w("AuthManager", "FirebaseAuth instance unavailable or uninitialized", e)
        null
    }

    init {
        // Sync with existing Firebase user if available
        firebaseAuth?.currentUser?.let { fbUser ->
            val profile = UserProfile(
                uid = fbUser.uid,
                displayName = fbUser.displayName ?: "Sinh viên",
                email = fbUser.email ?: "",
                photoUrl = fbUser.photoUrl?.toString(),
                isGoogleLinked = true
            )
            _currentUser.value = profile
            prefs.saveUserProfile(profile)
        }
    }

    /**
     * Đăng nhập bằng Google qua Jetpack Credential Manager
     */
    suspend fun signInWithGoogle(activity: Activity): Result<UserProfile> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId("dummy-client-id.apps.googleusercontent.com")
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                // If Firebase Auth is configured, sign in to Firebase
                var uid = "google_${email.hashCode()}"
                if (firebaseAuth != null && idToken.isNotBlank()) {
                    try {
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                        authResult.user?.let {
                            uid = it.uid
                        }
                    } catch (e: Exception) {
                        Log.w("AuthManager", "Firebase signInWithCredential skipped/failed, proceeding with Google profile: ${e.message}")
                    }
                }

                val profile = UserProfile(
                    uid = uid,
                    displayName = displayName,
                    email = email,
                    photoUrl = photoUrl,
                    isGoogleLinked = true
                )
                _currentUser.value = profile
                prefs.saveUserProfile(profile)
                Result.success(profile)
            } else {
                Result.failure(Exception("Phương thức đăng nhập không được hỗ trợ."))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Đã hủy đăng nhập Google."))
        } catch (e: Exception) {
            Log.e("AuthManager", "Google Sign-In error", e)
            val friendlyMsg = if (e.message?.contains("16") == true || e.message?.contains("No credential") == true) {
                "Chưa có tài khoản Google trên giả lập. Vui lòng dùng ô 'Đăng Nhập Nhanh Bằng Email' bên dưới để đồng bộ lịch ngay!"
            } else {
                "Lỗi kết nối Google: ${e.message}. Bạn có thể dùng ô 'Đăng Nhập Nhanh Bằng Email' bên dưới!"
            }
            Result.failure(Exception(friendlyMsg))
        }
    }

    /**
     * Đăng nhập trực tiếp bằng Email Sinh Viên (cho trường học / đại học hoặc thiết bị không có Google Play Services)
     */
    fun signInWithStudentEmail(email: String, displayName: String): Result<UserProfile> {
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@")) {
            return Result.failure(Exception("Địa chỉ Email không hợp lệ."))
        }
        val name = displayName.trim().ifBlank { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }
        val profile = UserProfile(
            uid = "student_${cleanEmail.hashCode()}",
            displayName = name,
            email = cleanEmail,
            photoUrl = null,
            isGoogleLinked = cleanEmail.endsWith("@gmail.com", ignoreCase = true)
        )
        _currentUser.value = profile
        prefs.saveUserProfile(profile)
        return Result.success(profile)
    }

    /**
     * Đăng xuất
     */
    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        prefs.clearUserProfile()
        _currentUser.value = null
    }

    fun updateLastSyncTime() {
        val now = System.currentTimeMillis()
        prefs.updateLastCalendarSyncTime(now)
        _currentUser.value = _currentUser.value?.copy(lastSyncTime = now)
    }
}
