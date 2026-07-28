package com.touchgrass.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EssayDao {

    @Insert
    suspend fun insert(essay: Essay): Long

    @Query("SELECT * FROM essays ORDER BY writtenAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<Essay>>

    @Query("SELECT COUNT(*) FROM essays")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM essays WHERE id = :id")
    fun observeById(id: Long): Flow<Essay?>

    /** Recently used words, so the generator can avoid immediate repeats. */
    @Query("SELECT word FROM essays ORDER BY writtenAt DESC LIMIT :limit")
    suspend fun recentWords(limit: Int = 30): List<String>
}

/** Row shape for the grouped per-app grant query. */
data class PerAppGrant(
    val pkg: String,
    val minutes: Int
)

@Dao
interface PassDao {

    @Insert
    suspend fun insert(grant: PassGrant): Long

    /** Total bonus minutes earned on a given budget day, across all apps. */
    @Query("SELECT COALESCE(SUM(minutesGranted), 0) FROM pass_grants WHERE dayKey = :dayKey")
    fun observeMinutesGranted(dayKey: String): Flow<Int>

    /**
     * Bonus minutes for the shared pool only — grants with no target app.
     * Used in SHARED mode so a targeted grant can't leak into it.
     */
    @Query(
        "SELECT COALESCE(SUM(minutesGranted), 0) FROM pass_grants " +
            "WHERE dayKey = :dayKey AND packageName IS NULL"
    )
    fun observeSharedMinutesGranted(dayKey: String): Flow<Int>

    /** All targeted grants for a day, as package → minutes. */
    @Query(
        "SELECT packageName AS pkg, SUM(minutesGranted) AS minutes FROM pass_grants " +
            "WHERE dayKey = :dayKey AND packageName IS NOT NULL GROUP BY packageName"
    )
    fun observePerAppGrants(dayKey: String): Flow<List<PerAppGrant>>

    @Query("SELECT * FROM pass_grants WHERE dayKey = :dayKey ORDER BY issuedAt DESC")
    fun observeGrants(dayKey: String): Flow<List<PassGrant>>

    @Query("SELECT COUNT(*) FROM pass_grants WHERE dayKey = :dayKey")
    suspend fun countForDay(dayKey: String): Int
}
