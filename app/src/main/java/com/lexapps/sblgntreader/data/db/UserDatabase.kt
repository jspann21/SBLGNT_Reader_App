package com.lexapps.sblgntreader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserVocabEntity::class], version = 1, exportSchema = true)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userVocabDao(): UserVocabDao

    companion object {
        const val DB_NAME = "user_vocabulary.db"

        fun build(context: Context): UserDatabase = Room.databaseBuilder(
            context.applicationContext,
            UserDatabase::class.java,
            DB_NAME
        ).build()
    }
}
