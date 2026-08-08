package com.youngjcu.pclab.data.repository

import com.youngjcu.pclab.data.local.FavouriteBuildEntity
import com.youngjcu.pclab.data.local.LearningDao
import com.youngjcu.pclab.data.local.LearningProgressEntity
import com.youngjcu.pclab.data.local.MissionResultEntity
import com.youngjcu.pclab.domain.model.BuildDraft
import com.youngjcu.pclab.domain.model.Evaluation
import com.youngjcu.pclab.domain.model.Mission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class LearningStatistics(
    val attemptCount: Int = 0,
    val averageScore: Int = 0,
    val completedMissionIds: Set<Int> = emptySet(),
    val favourites: List<FavouriteBuildEntity> = emptyList(),
    val recentResults: List<MissionResultEntity> = emptyList()
)

interface LearningRepository {
    fun observeStatistics(): Flow<LearningStatistics>
    suspend fun saveResult(mission: Mission, draft: BuildDraft, evaluation: Evaluation): Long
    suspend fun saveFavourite(label: String, draft: BuildDraft)
    suspend fun resetLearningData()
}

@Singleton
class RoomLearningRepository @Inject constructor(
    private val dao: LearningDao
) : LearningRepository {
    override fun observeStatistics(): Flow<LearningStatistics> = combine(
        dao.observeAttemptCount(),
        dao.observeAverageScore(),
        dao.observeProgress(),
        dao.observeFavourites(),
        dao.observeRecentResults()
    ) { attempts, average, progress, favourites, recent ->
        LearningStatistics(
            attemptCount = attempts,
            averageScore = (average ?: 0.0).toInt(),
            completedMissionIds = progress.filter { it.isCompleted }.map { it.missionId }.toSet(),
            favourites = favourites,
            recentResults = recent
        )
    }

    override suspend fun saveResult(mission: Mission, draft: BuildDraft, evaluation: Evaluation): Long {
        val now = System.currentTimeMillis()
        val resultId = dao.insertResult(
            MissionResultEntity(
                missionId = mission.id,
                missionTitle = mission.title,
                completedAt = now,
                score = evaluation.score,
                totalCost = evaluation.totalCost,
                isCompatible = evaluation.isCompatible,
                isWithinBudget = evaluation.isWithinBudget,
                performanceScore = evaluation.performanceScore,
                buildSummary = draft.summary()
            )
        )
        val previous = dao.getProgress(mission.id)
        dao.upsertProgress(
            LearningProgressEntity(
                missionId = mission.id,
                isCompleted = (previous?.isCompleted == true) || (evaluation.isCompatible && evaluation.isWithinBudget),
                bestScore = maxOf(previous?.bestScore ?: 0, evaluation.score),
                lastAttemptedAt = now
            )
        )
        return resultId
    }

    override suspend fun saveFavourite(label: String, draft: BuildDraft) {
        dao.insertFavourite(
            FavouriteBuildEntity(
                label = label,
                createdAt = System.currentTimeMillis(),
                totalCost = draft.totalCost,
                buildSummary = draft.summary()
            )
        )
    }

    override suspend fun resetLearningData() {
        dao.clearResults()
        dao.clearProgress()
        dao.clearFavourites()
    }

    private fun BuildDraft.summary(): String = selections.values.joinToString(" • ") { it.name }
}
