package com.example.treemap

import android.app.Application
import com.example.treemap.data.local.AppDatabase
import com.example.treemap.data.repository.TreeRepository
import com.example.treemap.data.repository.UserRepository

class TreeMapApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: TreeRepository by lazy { TreeRepository(database.treeDao(), this) }
    val userRepository: UserRepository by lazy { UserRepository(database.userDao(), this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: TreeMapApp
            private set
    }
}
