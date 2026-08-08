package com.youngjcu.pclab.data.remote

import retrofit2.http.GET

interface HardwareApi {
    @GET("data/cpu.json") suspend fun getCpus(): List<HardwarePartDto>
    @GET("data/gpu.json") suspend fun getGpus(): List<HardwarePartDto>
    @GET("data/motherboard.json") suspend fun getMotherboards(): List<HardwarePartDto>
    @GET("data/ram.json") suspend fun getRam(): List<HardwarePartDto>
    @GET("data/storage.json") suspend fun getStorage(): List<HardwarePartDto>
    @GET("data/psu.json") suspend fun getPsus(): List<HardwarePartDto>
    @GET("data/case.json") suspend fun getCases(): List<HardwarePartDto>
    @GET("data/missions.json") suspend fun getMissions(): List<MissionDto>
}

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
    val length: Int? = null,
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
