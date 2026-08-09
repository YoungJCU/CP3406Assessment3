package com.youngjcu.pclab.domain.rules

import com.youngjcu.pclab.domain.model.BuildDraft
import com.youngjcu.pclab.domain.model.HardwarePart
import com.youngjcu.pclab.domain.model.Mission
import com.youngjcu.pclab.domain.model.OutcomeStatus
import com.youngjcu.pclab.domain.model.PartCategory
import com.youngjcu.pclab.domain.model.PerformanceWeights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildEvaluatorTest {
    private val evaluator = BuildEvaluator()
    private val mission = Mission(
        id = 1, title = "AM5 workstation", description = "Test mission", budget = 1500,
        requiredSocket = "AM5", minimumPerformanceScore = 70,
        performanceWeights = PerformanceWeights(0.35, 0.10, 0.30, 0.25), requirements = emptyList(), hint = "Test"
    )

    @Test
    fun `compatible build meets budget and performance`() {
        val result = evaluator.evaluate(compatibleDraft(), mission)
        assertTrue(result.isCompatible)
        assertTrue(result.isWithinBudget)
        assertTrue(result.isMissionComplete)
        assertTrue(result.performanceScore >= mission.minimumPerformanceScore)
        assertEquals(100, result.score)
    }

    @Test
    fun `AM4 motherboard fails AM5 socket lesson`() {
        val draft = compatibleDraft().withPart(part(PartCategory.MOTHERBOARD, socket = "AM4", supportedRam = "DDR4", formFactor = "ATX"))
        val result = evaluator.evaluate(draft, mission)
        assertFalse(result.isCompatible)
        assertFalse(result.isMissionComplete)
        val socket = result.outcomes.first { it.title == "CPU and motherboard" }
        assertEquals(OutcomeStatus.FAIL, socket.status)
        assertTrue(socket.explanation.contains("AM5"))
        assertTrue(socket.explanation.contains("cannot connect"))
    }

    @Test
    fun `underpowered PSU fails headroom calculation`() {
        val result = evaluator.evaluate(compatibleDraft().withPart(part(PartCategory.PSU, psuWattage = 200)), mission)
        val power = result.outcomes.first { it.title == "Power supply" }
        assertEquals(OutcomeStatus.FAIL, power.status)
        assertTrue(power.explanation.contains("spare power"))
    }

    private fun compatibleDraft(): BuildDraft = BuildDraft().withPart(part(PartCategory.CPU, socket = "AM5", supportedRam = "DDR5", score = 90, power = 65))
        .withPart(part(PartCategory.MOTHERBOARD, socket = "AM5", supportedRam = "DDR5", formFactor = "Micro-ATX", score = 80, power = 45))
        .withPart(part(PartCategory.GPU, gpuLength = 245, score = 35))
        .withPart(part(PartCategory.RAM, ramGeneration = "DDR5", score = 90, power = 10))
        .withPart(part(PartCategory.STORAGE, score = 85, power = 5))
        .withPart(part(PartCategory.PSU, psuWattage = 650))
        .withPart(part(PartCategory.CASE, supportedFormFactors = listOf("Micro-ATX"), maxGpuLength = 300))

    private fun part(
        category: PartCategory, socket: String? = null, supportedRam: String? = null, ramGeneration: String? = null,
        formFactor: String? = null, supportedFormFactors: List<String> = emptyList(), maxGpuLength: Int? = null,
        gpuLength: Int? = null, psuWattage: Int? = null, score: Int = 70, power: Int = 0
    ) = HardwarePart(
        id = category.ordinal, category = category, name = category.label, brand = "Test", price = 100, power = power,
        performanceScore = score, learningNote = "Test note", socket = socket, supportedRam = supportedRam,
        ramGeneration = ramGeneration, formFactor = formFactor, supportedFormFactors = supportedFormFactors,
        maxGpuLengthMm = maxGpuLength, gpuLengthMm = gpuLength, psuWattage = psuWattage
    )
}
