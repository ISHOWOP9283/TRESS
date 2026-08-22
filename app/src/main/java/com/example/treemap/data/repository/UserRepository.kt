package com.example.treemap.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.treemap.data.local.UserDao
import com.example.treemap.data.model.UserAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch

class UserRepository(
    private val userDao: UserDao,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mangrove_auth_prefs", Context.MODE_PRIVATE)

    val allUsers: Flow<List<UserAccount>> = userDao.getAllUsers()
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    suspend fun seedDefaultUsersIfEmpty() {
        try {
            // Remove legacy user maya.lin@coastal.org if present
            userDao.deleteUserByEmail("maya.lin@coastal.org")

            val requiredUsers = listOf(
                UserAccount(
                    email = "admin",
                    username = "admin",
                    passwordHash = "admin",
                    displayName = "Chief Administrator",
                    role = UserAccount.ROLE_ADMIN,
                    isActive = true
                ),
                UserAccount(
                    email = "admin@mangrove.org",
                    username = "admin@mangrove.org",
                    passwordHash = "admin",
                    displayName = "Mangrove Admin Ops",
                    role = UserAccount.ROLE_ADMIN,
                    isActive = true
                ),
                UserAccount(
                    email = "manthansm@gmail.com",
                    username = "manthansm@gmail.com",
                    passwordHash = "user@123",
                    displayName = "Manthan SM",
                    role = UserAccount.ROLE_VOLUNTEER,
                    isActive = true,
                    isGoogleAccount = true
                ),
                UserAccount(
                    email = "gauravhp@gmail.com",
                    username = "gauravhp@gmail.com",
                    passwordHash = "user@123",
                    displayName = "Gaurav HP",
                    role = UserAccount.ROLE_VOLUNTEER,
                    isActive = true,
                    isGoogleAccount = true
                ),
                UserAccount(
                    email = "alex.rivera@volunteer.org",
                    username = "alex.rivera@volunteer.org",
                    passwordHash = "user@123",
                    displayName = "Alex Rivera",
                    role = UserAccount.ROLE_VOLUNTEER,
                    isActive = true
                )
            )

            for (user in requiredUsers) {
                val existing = userDao.getUserByEmailOrUsername(user.email)
                if (existing == null) {
                    userDao.insertUser(user)
                } else if (user.email == "manthansm@gmail.com" || user.email == "gauravhp@gmail.com") {
                    userDao.updateUser(existing.copy(passwordHash = "user@123", isActive = true))
                }
            }

            // Restore last logged in user if available
            val lastUserEmail = prefs.getString("last_logged_in_email", "admin")
            if (lastUserEmail != null) {
                val user = userDao.getUserByEmailOrUsername(lastUserEmail)
                if (user != null && user.isActive) {
                    _currentUser.value = user
                } else {
                    _currentUser.value = userDao.getUserByEmailOrUsername("admin")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun authenticate(emailOrUsername: String, password: String): UserAccount? {
        val trimmed = emailOrUsername.trim()
        val user = userDao.authenticate(trimmed, password)
        if (user != null && user.isActive) {
            _currentUser.value = user
            prefs.edit().putString("last_logged_in_email", user.email).apply()
            return user
        }
        return null
    }

    suspend fun directAccessByEmail(email: String): UserAccount {
        val trimmed = email.trim()
        val existing = userDao.getUserByEmailOrUsername(trimmed)
        if (existing != null) {
            _currentUser.value = existing
            prefs.edit().putString("last_logged_in_email", existing.email).apply()
            return existing
        }

        // Auto-grant access to new volunteer / random email
        val role = if (trimmed.equals("admin", ignoreCase = true) || trimmed.startsWith("admin@", ignoreCase = true)) {
            UserAccount.ROLE_ADMIN
        } else {
            UserAccount.ROLE_VOLUNTEER
        }

        val name = trimmed.substringBefore("@").replace(".", " ").capitalizeWords()
        val newUser = UserAccount(
            email = trimmed,
            username = trimmed,
            passwordHash = "volunteer123",
            displayName = if (name.isBlank()) "Community Volunteer" else name,
            role = role,
            isActive = true,
            isGoogleAccount = trimmed.contains("@gmail.com")
        )
        val id = userDao.insertUser(newUser)
        val saved = newUser.copy(id = id)
        _currentUser.value = saved
        prefs.edit().putString("last_logged_in_email", saved.email).apply()
        return saved
    }

    suspend fun grantAccess(email: String, displayName: String, role: String, password: String = "volunteer123"): UserAccount {
        val trimmed = email.trim()
        val existing = userDao.getUserByEmailOrUsername(trimmed)
        val userToSave = if (existing != null) {
            existing.copy(
                displayName = displayName.ifBlank { existing.displayName },
                role = role,
                isActive = true
            )
        } else {
            UserAccount(
                email = trimmed,
                username = trimmed,
                passwordHash = password.ifBlank { "volunteer123" },
                displayName = displayName.ifBlank { trimmed.substringBefore("@").capitalizeWords() },
                role = role,
                isActive = true,
                isGoogleAccount = trimmed.contains("@gmail.com")
            )
        }
        val id = userDao.insertUser(userToSave)
        return userToSave.copy(id = if (existing != null) existing.id else id)
    }

    suspend fun updateUser(user: UserAccount) {
        userDao.updateUser(user)
        if (_currentUser.value?.id == user.id) {
            _currentUser.value = user
        }
    }

    suspend fun deleteUser(id: Long) {
        userDao.deleteUserById(id)
    }

    suspend fun logout() {
        _currentUser.value = null
        prefs.edit().remove("last_logged_in_email").apply()
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
