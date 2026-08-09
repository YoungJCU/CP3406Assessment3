package com.youngjcu.pclab.ui

import com.youngjcu.pclab.data.repository.HardwareRepository
import com.youngjcu.pclab.domain.model.HardwareCatalogue
import com.youngjcu.pclab.domain.model.HardwarePart
import com.youngjcu.pclab.domain.model.Mission
import com.youngjcu.pclab.domain.model.PartCategory
import com.youngjcu.pclab.domain.model.PerformanceWeights
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuilderViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `repository catalogue is exposed to the Builder screen state`() = runTest {
        val cpu = HardwarePart(
            id = 1,
            category = PartCategory.CPU,
            name = "AMD Ryzen 5 7600",
            brand = "AMD",
            price = 289,
            power = 65,
            performanceScore = 90,
            learningNote = "AM5 supports DDR5.",
            socket = "AM5",
            supportedRam = "DDR5"
        )
        val catalogue = HardwareCatalogue(
            parts = PartCategory.entries.associateWith { category -> listOf(cpu.copy(category = category)) },
            missions = listOf(
                Mission(1, "Test mission", "Test", 1000, null, 50, PerformanceWeights(0.25, 0.25, 0.25, 0.25), emptyList(), "Test")
            )
        )
        val viewModel = BuilderViewModel(FakeHardwareRepository(catalogue))

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("AMD Ryzen 5 7600", viewModel.state.value.catalogue?.parts?.get(PartCategory.CPU)?.single()?.name)
    }

    private class FakeHardwareRepository(private val catalogue: HardwareCatalogue) : HardwareRepository {
        override suspend fun fetchCatalogue(): Result<HardwareCatalogue> = Result.success(catalogue)
    }
}
