package com.youngjcu.pclab.data.remote

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HardwareApi {
    @GET("repos/{owner}/{repository}/contents/{path}")
    suspend fun getFile(
        @Path("owner") owner: String,
        @Path("repository") repository: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") branch: String = "main"
    ): GitHubFileResponse
}

data class GitHubFileResponse(
    val content: String,
    val encoding: String
)

data class HardwarePartDto(
    val id: Int,
    val name: String,
    val brand: String,
    val price: Int,
    val power: Int,
    val performanceScore: Int,
    val learningNote: String,
    val socket: String? = null,
    val supportedRam: String? = null,
    val generation: String? = null,
    val capacityGb: Int? = null,
    val formFactor: String? = null,
    val supportedFormFactors: List<String> = emptyList(),
    val maxGpuLengthMm: Int? = null,
    @Json(name = "length") val gpuLengthMm: Int? = null,
    val wattage: Int? = null,
    val recommendedFor: List<String> = emptyList()
)

data class MissionDto(
    val id: Int,
    val title: String,
    val description: String,
    val budget: Int,
    val requiredSocket: String? = null,
    val minimumPerformanceScore: Int,
    val performanceWeights: PerformanceWeightsDto,
    val requirements: List<String>,
    val hint: String
)

data class PerformanceWeightsDto(
    val cpu: Double,
    val gpu: Double,
    val ram: Double,
    val storage: Double
)
