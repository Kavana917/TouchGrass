package com.touchgrass.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object — the interface you call instead of writing SQL by hand.
 * Room generates the implementation at build time.
 *
 * Returning a [Flow] makes the query *reactive*: the UI re-renders
 * automatically whenever the underlying table changes.
 */
@Dao
interface ScratchDao {

    @Query("SELECT * FROM scratch_notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ScratchNote>>

    @Query("SELECT COUNT(*) FROM scratch_notes")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(note: ScratchNote)

    @Query("DELETE FROM scratch_notes")
    suspend fun clear()
}
