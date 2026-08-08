package com.youngjcu.pclab.domain.rules

import com.youngjcu.pclab.domain.model.BuildDraft
import com.youngjcu.pclab.domain.model.Evaluation
import com.youngjcu.pclab.domain.model.HardwarePart
import com.youngjcu.pclab.domain.model.Mission
import com.youngjcu.pclab.domain.model.OutcomeStatus
import com.youngjcu.pclab.domain.model.PartCategory
import com.youngjcu.pclab.domain.model.RuleOutcome
import javax.inject.Inject

class BuildEvaluator @Inject constructor() {
    fun evaluate(draft: BuildDraft, mission: Mission): Evaluation {
        val cpu = draft.part(PartCategory.CPU)
        val board = draft.part(PartCategory.MOTHERBOARD)
        val gpu = draft.part(PartCategory.GPU)
        val ram = draft.part(PartCategory.RAM)
        val storage = draft.part(PartCategory.STORAGE)
        val psu = draft.part(PartCategory.PSU)
        val case = draft.part(PartCategory.CASE)
        val outcomes = buildList {
            add(socketOutcome(cpu, board))
            add(memoryOutcome(cpu, board, ram))
            add(caseOutcome(board, case))
            add(gpuFitOutcome(gpu, case))
            add(powerOutcome(draft, psu))
            add(budgetOutcome(draft, mission))
            mission.requiredSocket?.let { add(missionSocketOutcome(cpu, board, it)) }
        }
        val estimatedPower = draft.selections.values.sumOf(HardwarePart::power) + BASE_SYSTEM_POWER
        val performance = performanceScore(cpu, gpu, ram, storage, mission)
        val finalOutcomes = outcomes + performanceOutcome(performance, mission)
        val compatibilityPoints = if (finalOutcomes.none { it.status == OutcomeStatus.FAIL }) 40 else 0
        val budgetPoints = if (draft.totalCost <= mission.budget) 25 else 0
        val performancePoints = if (performance >= mission.minimumPerformanceScore) 25 else 0
        val completionPoints = if (draft.isComplete) 10 else 0
        return Evaluation(
            outcomes = finalOutcomes,
            totalCost = draft.totalCost,
            estimatedPower = estimatedPower,
            performanceScore = performance,
            score = compatibilityPoints + budgetPoints + performancePoints + completionPoints
        )
    }

    private fun socketOutcome(cpu: HardwarePart?, board: HardwarePart?) = when {
        cpu == null || board == null -> pending("CPU socket", "Choose a CPU and motherboard to check socket compatibility.")
        cpu.socket == board.socket -> pass("CPU socket", "${cpu.socket} matches: ${cpu.name} can be installed on ${board.name}.")
        else -> fail("CPU socket", "${cpu.name} uses ${cpu.socket}, while ${board.name} supports ${board.socket}. Choose matching sockets.")
    }

    private fun memoryOutcome(cpu: HardwarePart?, board: HardwarePart?, ram: HardwarePart?) = when {
        cpu == null || board == null || ram == null -> pending("Memory generation", "Choose CPU, motherboard and RAM to check DDR compatibility.")
        cpu.supportedRam == ram.ramGeneration && board.supportedRam == ram.ramGeneration ->
            pass("Memory generation", "${ram.ramGeneration} memory is supported by both the CPU and motherboard.")
        else -> fail("Memory generation", "${ram.name} is ${ram.ramGeneration}; this CPU/motherboard combination requires ${cpu.supportedRam} / ${board.supportedRam}.")
    }

    private fun caseOutcome(board: HardwarePart?, case: HardwarePart?) = when {
        board == null || case == null -> pending("Motherboard fit", "Choose a motherboard and case to check physical fit.")
        board.formFactor in case.supportedFormFactors -> pass("Motherboard fit", "${case.name} supports the ${board.formFactor} motherboard form factor.")
        else -> fail("Motherboard fit", "${board.formFactor} motherboards do not fit in ${case.name}; choose a compatible case.")
    }

    private fun gpuFitOutcome(gpu: HardwarePart?, case: HardwarePart?) = when {
        gpu == null || case == null -> pending("GPU clearance", "Choose a GPU and case to check graphics-card clearance.")
        (gpu.gpuLengthMm ?: 0) <= (case.maxGpuLengthMm ?: 0) -> pass("GPU clearance", "The ${gpu.gpuLengthMm} mm GPU fits within the ${case.maxGpuLengthMm} mm case limit.")
        else -> fail("GPU clearance", "The ${gpu.gpuLengthMm} mm GPU exceeds the ${case.maxGpuLengthMm} mm clearance of this case.")
    }

    private fun powerOutcome(draft: BuildDraft, psu: HardwarePart?): RuleOutcome {
        if (psu == null) return pending("Power headroom", "Choose a power supply to check safe wattage headroom.")
        val required = ((draft.selections.values.sumOf(HardwarePart::power) + BASE_SYSTEM_POWER) * POWER_HEADROOM).toInt()
        return if ((psu.psuWattage ?: 0) >= required) {
            pass("Power headroom", "${psu.psuWattage} W safely exceeds the estimated ${required} W requirement including 25% headroom.")
        } else {
            fail("Power headroom", "${psu.psuWattage} W is below the estimated ${required} W requirement. Choose a higher-capacity PSU.")
        }
    }

    private fun budgetOutcome(draft: BuildDraft, mission: Mission) = if (draft.totalCost <= mission.budget) {
        pass("Budget", "S$${draft.totalCost} is within the S$${mission.budget} mission budget.")
    } else {
        fail("Budget", "S$${draft.totalCost} exceeds the S$${mission.budget} mission budget by S$${draft.totalCost - mission.budget}.")
    }

    private fun missionSocketOutcome(cpu: HardwarePart?, board: HardwarePart?, requiredSocket: String) = when {
        cpu?.socket == requiredSocket && board?.socket == requiredSocket -> pass("Mission platform", "This mission requires $requiredSocket and both selected parts meet that requirement.")
        else -> fail("Mission platform", "This mission requires an $requiredSocket CPU and motherboard so you can learn the current platform.")
    }

    private fun performanceScore(
        cpu: HardwarePart?, gpu: HardwarePart?, ram: HardwarePart?, storage: HardwarePart?, mission: Mission
    ): Int {
        val weights = mission.performanceWeights
        return (listOf(
            (cpu?.performanceScore ?: 0) * weights.cpu,
            (gpu?.performanceScore ?: 0) * weights.gpu,
            (ram?.performanceScore ?: 0) * weights.ram,
            (storage?.performanceScore ?: 0) * weights.storage
        ).sum()).toInt()
    }

    private fun performanceOutcome(score: Int, mission: Mission) = if (score >= mission.minimumPerformanceScore) {
        pass("Mission performance", "Your weighted performance score is $score / 100, meeting the mission target of ${mission.minimumPerformanceScore}.")
    } else {
        fail("Mission performance", "Your weighted performance score is $score / 100; the mission target is ${mission.minimumPerformanceScore}. Review the components most important for this task.")
    }

    private fun pass(title: String, explanation: String) = RuleOutcome(title, OutcomeStatus.PASS, explanation)
    private fun fail(title: String, explanation: String) = RuleOutcome(title, OutcomeStatus.FAIL, explanation)
    private fun pending(title: String, explanation: String) = RuleOutcome(title, OutcomeStatus.WARNING, explanation)

    private companion object {
        const val BASE_SYSTEM_POWER = 120
        const val POWER_HEADROOM = 1.25
    }
}
