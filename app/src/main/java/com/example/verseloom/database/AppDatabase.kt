package com.example.verseloom.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserData::class, Writing::class], version = 6)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        val MIGRATION_2_3 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a new table with the updated schema
                database.execSQL("""
                    CREATE TABLE writings_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        content TEXT NOT NULL,
                        lastModified INTEGER NOT NULL
                    )
                """.trimIndent())

                // Copy data from the old table (if any) to the new table
                database.execSQL("""
                    INSERT INTO writings_new (id, userId, content, lastModified)
                    SELECT 1, 'default_user', content, lastModified
                    FROM writings
                """.trimIndent())

                // Drop the old table
                database.execSQL("DROP TABLE writings")

                // Rename the new table to the original name
                database.execSQL("ALTER TABLE writings_new RENAME TO writings")
            }
        }
    }
}