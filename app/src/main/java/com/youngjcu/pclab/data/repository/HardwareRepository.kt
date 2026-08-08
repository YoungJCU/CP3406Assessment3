package com.youngjcu.pclab.data.repository

import com.youngjcu.pclab.data.remote.HardwareRemoteDataSource
import com.youngjcu.pclab.data.remote.HardwarePartDto
import com.youngjcu.pclab.data.remote.MissionDto
import com.youngjcu.pclab.domain.model.HardwareCatalogue
import com.youngjcu.pclab.domain.model.HardwarePart
import com.youngjcu.pclab.domain.model.Mission
import com.youngjcu.pclab.domain.model.PartCategory
import com.youngjcu.pclab.domain.model.PerformanceWeights
import javax.inject.Inject
import javax.inject.Singleton

interface HardwareRepository {
    suspend fun fetchCatalogue(): Result<HardwareCatalogue>
}

@Singleton
class GitHubHardwareRepository @Inject constructor(
    private val remoteDataSource: HardwareRemoteDataSource
) : HardwareRepository {
    override suspend fun fetchCatalogue(): Result<HardwareCatalogue> = runCatching {
        val remote = remoteDataSource.loadCatalogue()
        HardwareCatalogue(
            parts = mapOf(
                PartCategory.CPU to remote.cpus.map { it.toDomain(PartCategory.CPU) },
                PartCategory.GPU to remote.gpus.map { it.toDomain(PartCategory.GPU) },
                PartCategory.MOTHERBOARD to remote.motherboards.map { it.toDomain(PartCategory.MOTHERBOARD) },
                PartCategory.RAM to remote.ram.map { it.toDomain(PartCategory.RAM) },
                PartCategory.STORAGE to remote.storage.map { it.toDomain(PartCategory.STORAGE) },
                PartCategory.PSU to remote.psus.map { it.toDomain(PartCategory.PSU) },
                PartCategory.CASE to remote.cases.map { it.toDomain(PartCategory.CASE) }
            ),
            missions = remote.missions.map(::mapMission)
        )
    }

    private fun mapMission(dto: MissionDto) = Mission(
        id = dto.id,
        title = dto.title,
        description = dto.description,
        budget = dto.budget,
        requiredSocket = dto.requiredSocket,
        minimumPerformanceScore = dto.minimumPerformanceScore,
        performanceWeights = PerformanceWeights(
            cpu = dto.performanceWeights.cpu,
            gpu = dto.performanceWeights.gpu,
            ram = dto.performanceWeights.ram,
            storage = dto.performanceWeights.storage
        ),
        requirements = dto.requirements,
        hint = dto.hint
    )

    private fun HardwarePartDto.toDomain(category: PartCategory) = HardwarePart(
        id = id,
        category = category,
        name = name,
        brand = brand,
        price = price,
        power = power,
        performanceScore = performanceScore,
        learningNote = learningNote,
        socket = socket,
        supportedRam = supportedRam,
        ramGeneration = generation,
        ramCapacityGb = capacityGb,
        formFactor = formFactor,
        supportedFormFactors = supportedFormFactors,
        maxGpuLengthMm = maxGpuLengthMm,
        gpuLengthMm = length,
        psuWattage = wattage,
        storageCapacityGb = capacityGb,
        recommendedFor = recommendedFor
    )

}
