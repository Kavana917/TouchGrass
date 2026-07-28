package com.touchgrass.app.core.data.db

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {

    /** Reactive: the UI re-renders whenever the monitor writes a new total. */
    @Query("SELECT * FROM usage_days WHERE date = :date")
    fun observeDay(date: String): Flow<UsageDay?>

    @Query("SELECT * FROM usage_days WHERE date = :date")
    suspend fun getDay(date: String): UsageDay?

    @Query("SELECT * FROM usage_days ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<UsageDay>>

    @Upsert
    suspend fun upsert(day: UsageDay)

    @Query("DELETE FROM usage_days")
    suspend fun clear()
}
