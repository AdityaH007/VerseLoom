package com.example.verseloom.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM UserData")
    fun getAll(): Flow<List<UserData>>

    @Query("SELECT * FROM UserData WHERE uid IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): Flow<List<UserData>>

    @Insert
    suspend fun insertAll(vararg userData: UserData)

    @Update
    suspend fun update(userData: UserData)

    @Delete
    suspend fun delete(userData: UserData)

    @Query("SELECT * FROM UserData WHERE uid = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): UserData?

    @Query("DELETE FROM UserData")
    suspend fun deleteAll()

    //writings
    @Insert
    suspend fun upsertWriting(writing: Writing): Long

    @Query("SELECT * FROM writings WHERE id= 'current'")
    suspend fun getCurrentWriting(): Writing?



    @Query("SELECT * FROM writings WHERE userId = :userId ORDER BY lastModified DESC")
    suspend fun getWritingsForUser(userId: String): List<Writing>

    @Query("SELECT * FROM writings WHERE userId = :userId AND id = :writingId")
    suspend fun getWritingById(userId: String, writingId: Long): Writing?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(userData: UserData)
}