package com.joshrpr.knifetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface KnifeDao {
    @Query(
        """
        SELECT k.*,
            (SELECT s.angle FROM sharpenings s WHERE s.knifeId = k.id
                ORDER BY s.sharpenedAt DESC LIMIT 1) AS lastAngle,
            (SELECT MAX(s.sharpenedAt) FROM sharpenings s WHERE s.knifeId = k.id) AS lastSharpenedAt
        FROM knives k
        ORDER BY k.name COLLATE NOCASE
        """
    )
    fun observeKnifeSummaries(): Flow<List<KnifeSummary>>

    @Query("SELECT * FROM knives WHERE id = :id")
    fun observeKnife(id: Long): Flow<Knife?>

    @Query("SELECT * FROM knives WHERE id = :id")
    suspend fun getKnife(id: Long): Knife?

    @Upsert
    suspend fun upsertKnife(knife: Knife): Long

    @Delete
    suspend fun deleteKnife(knife: Knife)

    @Query("SELECT * FROM sharpenings WHERE knifeId = :knifeId ORDER BY sharpenedAt DESC")
    fun observeSharpenings(knifeId: Long): Flow<List<Sharpening>>

    @Insert
    suspend fun insertSharpening(sharpening: Sharpening): Long

    @Delete
    suspend fun deleteSharpening(sharpening: Sharpening)
}
