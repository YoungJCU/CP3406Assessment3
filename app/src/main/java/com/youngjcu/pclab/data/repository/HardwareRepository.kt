package com.youngjcu.pclab.data.repository

import android.util.Base64
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.youngjcu.pclab.data.remote.HardwareApi
import com.youngjcu.pclab.data.remote.HardwarePartDto
import com.youngjcu.pclab.data.remote.MissionDto
import com.youngjcu.pclab.domain.model.HardwareCatalogue
import com.youngjcu.pclab.domain.model.HardwarePart
import com.youngjcu.pclab.domain.model.Mission
import com.youngjcu.pclab.domain.model.PartCategory
import com.youngjcu.pclab.domain.model.PerformanceWeights
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

interface HardwareRepository {
    suspend fun fetchCatalogue(): Result<HardwareCatalogue>
}

@Singleton
class GitHubHardwareRepository @Inject constructor(
    private val api: HardwareApi,
    private val moshi: Moshi
) : HardwareRepository {
    override suspend fun fetchCatalogue(): Result<HardwareCatalogue> = runCatching {
        coroutineScope {
            val parts = PartCategory.entries.associateWith { category ->
                async { fetchParts(category) }
            }.mapValues { (_, deferred) -> deferred.await() }
            HardwareCatalogue(parts = parts, missions = fetchMissions())
        }
    }

    private suspend fun fetchParts(category: PartCategory): List<HardwarePart> {
        val type = Types.newParameterizedType(List::class.java, HardwarePartDto::class.java)
        val dtos = moshi.adapter<List<HardwarePartDto>>(type).fromJson(fetchText("${category.fileName}.json"))
            ?: error("The ${category.label} catalogue was empty.")
        return dtos.map { it.toDomain(category) }
    }

    private suspend fun fetchMissions(): List<Mission> {
        val type = Types.newParameterizedType(List::class.java, MissionDto::class.java)
        val dtos = moshi.adapter<List<MissionDto>>(type).fromJson(fetchText("missions.json"))
            ?: error("The mission catalogue was empty.")
        return dtos.map { dto ->
            Mission(
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
        }
    }

    private suspend fun fetchText(fileName: String): String {
        val response = api.getFile(OWNER, REPOSITORY, "data/$fileName")
        check(response.encoding.equals("base64", ignoreCase = true)) { "Unexpected GitHub content encoding." }
        return Base64.decode(response.content.replace("\n", ""), Base64.DEFAULT).decodeToString()
    }

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
        gpuLengthMm = gpuLengthMm,
        psuWattage = wattage,
        storageCapacityGb = capacityGb,
        recommendedFor = recommendedFor
    )

    private val PartCategory.fileName: String
        get() = when (this) {
            PartCategory.CPU -> "cpu"
            PartCategory.MOTHERBOARD -> "motherboard"
            PartCategory.GPU -> "gpu"
            PartCategory.RAM -> "ram"
            PartCategory.STORAGE -> "storage"
            PartCategory.PSU -> "psu"
            PartCategory.CASE -> "case"
        }

    private companion object {
        const val OWNER = "YoungJCU"
        const val REPOSITORY = "buildpc-data"
    }
}
