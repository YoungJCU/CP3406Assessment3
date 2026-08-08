package com.youngjcu.pclab.data.remote

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteHardwareCatalogue(
    val cpus: List<HardwarePartDto>,
    val gpus: List<HardwarePartDto>,
    val motherboards: List<HardwarePartDto>,
    val ram: List<HardwarePartDto>,
    val storage: List<HardwarePartDto>,
    val psus: List<HardwarePartDto>,
    val cases: List<HardwarePartDto>,
    val missions: List<MissionDto>
)

/** Reads the versioned public learning catalogue from GitHub Raw. */
@Singleton
class HardwareRemoteDataSource @Inject constructor(
    private val api: HardwareApi
) {
    suspend fun loadCatalogue(): RemoteHardwareCatalogue = coroutineScope {
        val cpus = async { api.getCpus() }
        val gpus = async { api.getGpus() }
        val motherboards = async { api.getMotherboards() }
        val ram = async { api.getRam() }
        val storage = async { api.getStorage() }
        val psus = async { api.getPsus() }
        val cases = async { api.getCases() }
        val missions = async { api.getMissions() }
        RemoteHardwareCatalogue(
            cpus = cpus.await(),
            gpus = gpus.await(),
            motherboards = motherboards.await(),
            ram = ram.await(),
            storage = storage.await(),
            psus = psus.await(),
            cases = cases.await(),
            missions = missions.await()
        )
    }
}
