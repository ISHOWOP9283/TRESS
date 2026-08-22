package com.example.treemap.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val username: String = email,
    val passwordHash: String = "admin", // simple password for testing & local auth
    val displayName: String,
    val role: String = ROLE_VOLUNTEER, // "ADMIN" or "VOLUNTEER"
    val isActive: Boolean = true,
    val isGoogleAccount: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean
        get() = role.equals(ROLE_ADMIN, ignoreCase = true)

    val roleLabel: String
        get() = if (isAdmin) "Administrator" else "Community Volunteer"

    companion object {
        const val ROLE_ADMIN = "ADMIN"
        const val ROLE_VOLUNTEER = "VOLUNTEER"
    }
}
