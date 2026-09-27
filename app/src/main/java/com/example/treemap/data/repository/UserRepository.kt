package com.example.treemap.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.treemap.data.local.UserDao
import com.example.treemap.data.model.UserAccount
import com.example.treemap.data.remote.SupabaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserRepository(
    private val userDao: UserDao,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mangrove_auth_prefs", Context.MODE_PRIVATE)
    private val supabaseService = SupabaseService(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val tag = "UserRepository"

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

            // Sync with Supabase cloud users if connected
            if (supabaseService.isConfigured()) {
                syncUsersWithCloud()
            }

            // Restore last logged in user if previously saved
            val lastUserEmail = prefs.getString("last_logged_in_email", null)
            if (lastUserEmail != null) {
                val user = userDao.getUserByEmailOrUsername(lastUserEmail)
                if (user != null && user.isActive) {
                    _currentUser.value = user
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun authenticate(emailOrUsername: String, password: String): UserAccount? = withContext(Dispatchers.IO) {
        val trimmed = emailOrUsername.trim()
        var user = userDao.authenticate(trimmed, password)

        // If local check failed and Supabase is configured, try pulling latest users from Supabase
        if (user == null && supabaseService.isConfigured()) {
            try {
                syncUsersWithCloud()
                user = userDao.authenticate(trimmed, password)
            } catch (e: Exception) {
                Log.w(tag, "Remote auth sync check failed: ${e.message}")
            }
        }

        if (user != null && user.isActive) {
            _currentUser.value = user
            prefs.edit().putString("last_logged_in_email", user.email).apply()
            return@withContext user
        }
        return@withContext null
    }

    suspend fun directAccessByEmail(email: String): UserAccount = withContext(Dispatchers.IO) {
        val trimmed = email.trim()
        val existing = userDao.getUserByEmailOrUsername(trimmed)
        if (existing != null) {
            _currentUser.value = existing
            prefs.edit().putString("last_logged_in_email", existing.email).apply()
            return@withContext existing
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

        // Sync new user to Supabase
        if (supabaseService.isConfigured()) {
            scope.launch {
                try {
                    supabaseService.upsertUser(saved)
                } catch (e: Exception) {
                    Log.e(tag, "Failed to push new user to Supabase", e)
                }
            }
        }

        return@withContext saved
    }

    suspend fun grantAccess(email: String, displayName: String, role: String, password: String = "volunteer123"): UserAccount = withContext(Dispatchers.IO) {
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
        val finalUser = userToSave.copy(id = if (existing != null) existing.id else id)

        if (supabaseService.isConfigured()) {
            scope.launch {
                try {
                    supabaseService.upsertUser(finalUser)
                } catch (e: Exception) {
                    Log.e(tag, "Failed to sync user to Supabase", e)
                }
            }
        }

        return@withContext finalUser
    }

    suspend fun updateUser(user: UserAccount) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
        if (_currentUser.value?.id == user.id) {
            _currentUser.value = user
        }
        if (supabaseService.isConfigured()) {
            scope.launch {
                try {
                    supabaseService.upsertUser(user)
                } catch (e: Exception) {
                    Log.e(tag, "Failed to update user in Supabase", e)
                }
            }
        }
    }

    suspend fun deleteUser(id: Long) = withContext(Dispatchers.IO) {
        userDao.deleteUserById(id)
        if (supabaseService.isConfigured()) {
            scope.launch {
                try {
                    supabaseService.deleteUser(id)
                } catch (e: Exception) {
                    Log.e(tag, "Failed to delete user in Supabase", e)
                }
            }
        }
    }

    suspend fun syncUsersWithCloud(): Result<Int> = withContext(Dispatchers.IO) {
        if (!supabaseService.isConfigured()) {
            return@withContext Result.failure(Exception("Supabase is not configured"))
        }

        try {
            val remoteResult = supabaseService.fetchAllUsers()
            if (remoteResult.isFailure) {
                return@withContext Result.failure(remoteResult.exceptionOrNull() ?: Exception("Error fetching users"))
            }

            val remoteUsers = remoteResult.getOrDefault(emptyList())
            for (user in remoteUsers) {
                val existing = userDao.getUserByEmailOrUsername(user.email)
                if (existing == null) {
                    userDao.insertUser(user)
                } else {
                    userDao.updateUser(user.copy(id = existing.id))
                }
            }

            // Push any local users that are not in remote
            val localUsers = userDao.getAllUsers().firstOrNull() ?: emptyList()
            val remoteEmails = remoteUsers.map { it.email.lowercase() }.toSet()
            for (local in localUsers) {
                if (!remoteEmails.contains(local.email.lowercase())) {
                    supabaseService.upsertUser(local)
                }
            }

            Result.success(remoteUsers.size)
        } catch (e: Exception) {
            Log.e(tag, "Error syncing users with Supabase", e)
            Result.failure(e)
        }
    }

    suspend fun logout() {
        _currentUser.value = null
        prefs.edit().remove("last_logged_in_email").apply()
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
