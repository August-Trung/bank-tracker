package com.banktracker.vn.data.local

import android.content.Context
import androidx.room.*
import com.banktracker.vn.data.model.Transaction

@Database(
    entities = [Transaction::class],
    version = 2,  // TĂNG VERSION vì đổi schema
    exportSchema = true
)
// XÓA @TypeConverters vì không cần nữa
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bank_tracker_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}