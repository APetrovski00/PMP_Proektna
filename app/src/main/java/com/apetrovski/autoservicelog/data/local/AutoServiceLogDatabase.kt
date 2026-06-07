package com.apetrovski.autoservicelog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.apetrovski.autoservicelog.data.local.dao.CarDao
import com.apetrovski.autoservicelog.data.local.dao.LocalUserDao
import com.apetrovski.autoservicelog.data.local.dao.WorksheetDao
import com.apetrovski.autoservicelog.data.local.dao.WorksheetEntryDao
import com.apetrovski.autoservicelog.data.local.entities.CarEntity
import com.apetrovski.autoservicelog.data.local.entities.LocalUserEntity
import com.apetrovski.autoservicelog.data.local.entities.WorksheetEntity
import com.apetrovski.autoservicelog.data.local.entities.WorksheetEntryEntity

@Database(
    entities = [
        LocalUserEntity::class,
        CarEntity::class,
        WorksheetEntity::class,
        WorksheetEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AutoServiceLogDatabase : RoomDatabase() {
    abstract fun localUserDao(): LocalUserDao
    abstract fun carDao(): CarDao
    abstract fun worksheetDao(): WorksheetDao
    abstract fun worksheetEntryDao(): WorksheetEntryDao

    companion object {
        @Volatile
        private var instance: AutoServiceLogDatabase? = null

        fun getDatabase(context: Context): AutoServiceLogDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AutoServiceLogDatabase::class.java,
                    "auto_service_log.db"
                ).build().also { instance = it }
            }
        }
    }
}
