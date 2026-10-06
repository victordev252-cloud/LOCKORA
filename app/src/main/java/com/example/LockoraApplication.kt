package com.example

import android.app.Application
import androidx.room.Room
import com.example.db.LockoraDatabase
import com.example.repository.AppLockRepository
import com.example.security.SecurityManager

class LockoraApplication : Application() {

    lateinit var database: LockoraDatabase
        private set

    lateinit var securityManager: SecurityManager
        private set

    lateinit var repository: AppLockRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            LockoraDatabase::class.java,
            "lockora_room.db"
        ).fallbackToDestructiveMigration().build()

        securityManager = SecurityManager.get(this)
        repository = AppLockRepository(this, database.dao(), securityManager)
    }

    companion object {
        lateinit var instance: LockoraApplication
            private set
        const val VERSION_NAME = "1.0.0"
        const val VERSION_CODE = 1
    }
}
