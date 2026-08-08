package com.youngjcu.pclab.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "mission_results")
data class MissionResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val missionId: Int,
    val missionTitle: String,
    val completedAt: Long,
    val score: Int,
    val totalCost: Int,
    val isCompatible: Boolean,
    val isWithinBudget: Boolean,
    val performanceScore: Int,
    val buildSummary: String
)

@Entity(tableName = "learning_progress")
data class LearningProgressEntity(
    @PrimaryKey val missionId: Int,
    val isCompleted: Boolean,
    val bestScore: Int,
    val lastAttemptedAt: Long
)

@Entity(tableName = "favourite_builds")
data class FavouriteBuildEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val createdAt: Long,
    val totalCost: Int,
    val buildSummary: String
)

@Dao
interface LearningDao {
    @Insert
    suspend fun insertResult(result: MissionResultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: LearningProgressEntity)

    @Query("SELECT * FROM learning_progress WHERE missionId = :missionId LIMIT 1")
    suspend fun getProgress(missionId: Int): LearningProgressEntity?

    @Insert
    suspend fun insertFavourite(build: FavouriteBuildEntity)

    @Query("SELECT * FROM mission_results ORDER BY completedAt DESC LIMIT 5")
    fun observeRecentResults(): Flow<List<MissionResultEntity>>

    @Query("SELECT * FROM learning_progress")
    fun observeProgress(): Flow<List<LearningProgressEntity>>

    @Query("SELECT * FROM favourite_builds ORDER BY createdAt DESC")
    fun observeFavourites(): Flow<List<FavouriteBuildEntity>>

    @Query("SELECT COUNT(*) FROM mission_results")
    fun observeAttemptCount(): Flow<Int>

    @Query("SELECT AVG(score) FROM mission_results")
    fun observeAverageScore(): Flow<Double?>

    @Query("DELETE FROM mission_results")
    suspend fun clearResults()

    @Query("DELETE FROM learning_progress")
    suspend fun clearProgress()

    @Query("DELETE FROM favourite_builds")
    suspend fun clearFavourites()
}

@Database(
    entities = [MissionResultEntity::class, LearningProgressEntity::class, FavouriteBuildEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun learningDao(): LearningDao
}
