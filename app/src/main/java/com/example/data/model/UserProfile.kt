package com.example.data.model

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isGoogleLinked: Boolean = true,
    val lastSyncTime: Long? = null
)
