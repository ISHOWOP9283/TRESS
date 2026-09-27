package com.example.treemap.data.remote

import android.content.Context
import android.util.Log
import com.example.treemap.data.model.TreeEntry
import com.example.treemap.data.model.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class SupabaseService(private val context: Context) {

    private val tag = "SupabaseService"

    private fun getBaseUrl(): String {
        return SupabaseConfig.getSupabaseUrl(context).trim().removeSuffix("/")
    }

    private fun getApiKey(): String {
        return SupabaseConfig.getSupabaseAnonKey(context).trim()
    }

    fun isConfigured(): Boolean {
        return SupabaseConfig.isConfigured(context)
    }

    /**
     * Tests connectivity to Supabase project by querying rest/v1/entries
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase credentials are not configured."))
        }
        val baseUrl = getBaseUrl()
        val apiKey = getApiKey()

        try {
            val url = URL("$baseUrl/rest/v1/entries?select=id&limit=1")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val code = conn.responseCode
            if (code in 200..299) {
                Result.success("Connected successfully to Supabase (HTTP $code)")
            } else {
                val errorBody = readStream(conn.errorStream)
                Result.failure(Exception("HTTP $code: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(tag, "testConnection failed", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads an image file to Supabase Storage bucket 'tree-photos' and returns the public URL
     */
    suspend fun uploadImage(imageFile: File, bucket: String = "tree-photos"): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase is not configured"))
        }
        if (!imageFile.exists() || imageFile.length() == 0L) {
            return@withContext Result.failure(Exception("Image file does not exist or is empty"))
        }

        val baseUrl = getBaseUrl()
        val apiKey = getApiKey()
        val fileName = "field_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"

        try {
            val uploadUrl = URL("$baseUrl/storage/v1/object/$bucket/$fileName")
            val conn = (uploadUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "image/jpeg")
                setRequestProperty("x-upsert", "true")
                connectTimeout = 15000
                readTimeout = 20000
            }

            FileInputStream(imageFile).use { input ->
                conn.outputStream.use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val publicUrl = "$baseUrl/storage/v1/object/public/$bucket/$fileName"
                Log.d(tag, "Image successfully uploaded to Supabase: $publicUrl")
                Result.success(publicUrl)
            } else {
                val errorBody = readStream(conn.errorStream)
                Log.e(tag, "Image upload failed HTTP $code: $errorBody")
                Result.failure(Exception("Upload failed HTTP $code: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error uploading image to Supabase", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches all entries from Supabase entries table
     */
    suspend fun fetchAllEntries(): Result<List<TreeEntry>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase not configured"))
        }
        val baseUrl = getBaseUrl()
        val apiKey = getApiKey()

        try {
            val url = URL("$baseUrl/rest/v1/entries?select=*&order=date.desc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 10000
                readTimeout = 12000
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val responseStr = readStream(conn.inputStream)
                val jsonArray = JSONArray(responseStr)
                val entries = mutableListOf<TreeEntry>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawId = obj.optLong("id", 0L)
                    val resolvedId = if (rawId != 0L) rawId else System.currentTimeMillis()
                    entries.add(
                        TreeEntry(
                            id = resolvedId,
                            lat = obj.optDouble("lat", 0.0),
                            lng = obj.optDouble("lng", 0.0),
                            category = obj.optStringSafe("category", "fair_growth"),
                            title = obj.optStringSafe("title", "Mangrove Observation"),
                            species = obj.optStringSafe("species", "Rhizophora mangle"),
                            zoneId = obj.optStringSafe("zone_id", "zone_a"),
                            imageUrls = obj.optStringSafe("image_urls", ""),
                            notes = obj.optStringSafe("notes", ""),
                            reporter = obj.optStringSafe("reporter", "Field Volunteer"),
                            date = obj.optLong("date", resolvedId)
                        )
                    )
                }
                Log.d(tag, "Fetched ${entries.size} entries from Supabase")
                Result.success(entries)
            } else {
                val errorBody = readStream(conn.errorStream)
                Result.failure(Exception("Fetch failed HTTP $code: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(tag, "fetchAllEntries error", e)
            Result.failure(e)
        }
    }

    /**
     * Upserts an entry to Supabase (creates or updates)
     */
    suspend fun upsertEntry(entry: TreeEntry): Result<TreeEntry> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase not configured"))
        }
        val baseUrl = getBaseUrl()
        val apiKey = getApiKey()

        try {
            val url = URL("$baseUrl/rest/v1/entries?on_conflict=id")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
                connectTimeout = 10000
                readTimeout = 12000
            }

            val payload = JSONObject().apply {
                put("id", entry.id)
                put("title", entry.title)
                put("species", entry.species)
                put("category", entry.category)
                put("notes", entry.notes.orEmpty())
                put("reporter", entry.reporter)
                put("zone_id", entry.zoneId)
                put("lat", entry.lat)
                put("lng", entry.lng)
                put("image_urls", entry.imageUrls)
                put("date", entry.date)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                Log.d(tag, "Upserted entry ${entry.id} (${entry.title}) to Supabase")
                Result.success(entry)
            } else {
                val errorBody = readStream(conn.errorStream)
                Log.e(tag, "Upsert failed HTTP $code: $errorBody")
                Result.failure(Exception("Upsert failed HTTP $code: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(tag, "upsertEntry error", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes an entry from Supabase entries table by ID
     */
    suspend fun deleteEntry(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase not configured"))
        }
        val baseUrl = getBaseUrl()
        val apiKey = getApiKey()

        try {
            val url = URL("$baseUrl/rest/v1/entries?id=eq.$id")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val code = conn.responseCode
            if (code in 200..299) {
                Result.success(true)
            } else {
                val errorBody = readStream(conn.errorStream)
                Result.failure(Exception("Delete failed HTTP $code: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(tag, "deleteEntry error", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches all user accounts from Supabase user_accounts table
     */
    suspend fun fetchAllUsers(): Result<List<UserAccount>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase not configured"))
        }
        val baseUrl = getBaseUrl()
        val apiKey = getApiKey()

        try {
            val url = URL("$baseUrl/rest/v1/user_accounts?select=*&order=created_at.desc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 10000
                readTimeout = 12000
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val responseStr = readStream(conn.inputStream)
                val jsonArray = JSONArray(responseStr)
                val users = mutableListOf<UserAccount>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    users.add(
                        UserAccount(
                            id = obj.optLong("id", System.currentTimeMillis()),
                            email = obj.optString("email", ""),
                            username = obj.optString("username", ""),
                            passwordHash = obj.optString("password_hash", ""),
                            displayName = obj.optString("display_name", "Field User"),
                            role = obj.optString("role", UserAccount.ROLE_VOLUNTEER),
                            isActive = obj.optBoolean("is_active", true),
                            isGoogleAccount = obj.optBoolean("is_google_account", false),
                            createdAt = obj.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
                Result.success(users)
            } else {
                val errorBody = readStream(conn.errorStream)
                Result.failure(Exception("Fetch users failed HTTP $code: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(tag, "fetchAllUsers error", e)
            Result.failure(e)
        }
    }

    /**
     * Upserts a user account to Supabase
     */
    suspend fun upsertUser(user: UserAccount): Result<UserAccount> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase not configured"))
        }
        val baseUrl = getBaseUrl()
        val apiKey = getApiKey()

        try {
            val url = URL("$baseUrl/rest/v1/user_accounts?on_conflict=id")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
                connectTimeout = 10000
                readTimeout = 12000
            }

            val payload = JSONObject().apply {
                put("id", user.id)
                put("email", user.email)
                put("username", user.username)
                put("password_hash", user.passwordHash)
                put("display_name", user.displayName)
                put("role", user.role)
                put("is_active", user.isActive)
                put("is_google_account", user.isGoogleAccount)
                put("created_at", user.createdAt)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                Result.success(user)
            } else {
                val errorBody = readStream(conn.errorStream)
                Result.failure(Exception("Upsert user failed HTTP $code: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(tag, "upsertUser error", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a user account from Supabase by ID
     */
    suspend fun deleteUser(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase not configured"))
        }
        val baseUrl = getBaseUrl()
        val apiKey = getApiKey()

        try {
            val url = URL("$baseUrl/rest/v1/user_accounts?id=eq.$id")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val code = conn.responseCode
            if (code in 200..299) {
                Result.success(true)
            } else {
                val errorBody = readStream(conn.errorStream)
                Result.failure(Exception("Delete user failed HTTP $code: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(tag, "deleteUser error", e)
            Result.failure(e)
        }
    }

    private fun readStream(inputStream: java.io.InputStream?): String {
        if (inputStream == null) return ""
        return try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun JSONObject.optStringSafe(key: String, default: String = ""): String {
        if (isNull(key)) return default
        val v = optString(key, default)
        return if (v == "null") default else v
    }
}
