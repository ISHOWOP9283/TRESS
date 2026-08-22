package com.example.treemap.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.treemap.data.model.TreeEntry
import com.example.treemap.data.model.UserAccount

@Database(entities = [TreeEntry::class, UserAccount::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun treeDao(): TreeDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mangrove_mapper_v3.db"
                )
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
