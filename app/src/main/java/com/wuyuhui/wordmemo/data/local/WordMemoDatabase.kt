package com.wuyuhui.wordmemo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WordLibraryEntity::class,
        WordEntity::class,
        ReviewLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WordMemoDatabase : RoomDatabase() {

    abstract fun libraryDao(): WordLibraryDao

    abstract fun wordDao(): WordDao

    abstract fun reviewLogDao(): ReviewLogDao

    companion object {
        private const val DB_NAME = "wordmemo.db"

        @Volatile
        private var instance: WordMemoDatabase? = null

        fun get(context: Context): WordMemoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WordMemoDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
