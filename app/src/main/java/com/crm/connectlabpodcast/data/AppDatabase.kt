package com.crm.connectlabpodcast.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.security.Provider
import javax.inject.Inject

@Database(
    entities = [Record::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // record dao
    abstract fun recordDao(): RecordDao

    companion object{
        @Volatile  private var instance : AppDatabase? = null

        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK){
            instance ?: buildDatabse(context).also {
                instance = it
            }

        }

        private fun buildDatabse(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "AppDatabase"
        ).build()
    }

}