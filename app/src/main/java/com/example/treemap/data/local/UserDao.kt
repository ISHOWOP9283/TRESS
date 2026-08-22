package com.example.treemap.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.treemap.data.model.UserAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:emailOrUsername) OR LOWER(username) = LOWER(:emailOrUsername) LIMIT 1")
    suspend fun getUserByEmailOrUsername(emailOrUsername: String): UserAccount?

    @Query("SELECT * FROM users WHERE (LOWER(email) = LOWER(:query) OR LOWER(username) = LOWER(:query)) AND passwordHash = :password LIMIT 1")
    suspend fun authenticate(query: String, password: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserAccount>)

    @Update
    suspend fun updateUser(user: UserAccount)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    @Query("DELETE FROM users WHERE LOWER(email) = LOWER(:email) OR LOWER(username) = LOWER(:email)")
    suspend fun deleteUserByEmail(email: String)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}
