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
        cpu == null || board == null -> pending("CPU and motherboard", "Choose a CPU and motherboard. We will check whether their connection types match.")
        cpu.socket == board.socket -> pass("CPU and motherboard", "Both parts use ${cpu.socket}. The CPU can plug into this motherboard.")
        else -> fail("CPU and motherboard", "This CPU plugs into ${cpu.socket}, but this motherboard has ${board.socket}. They cannot connect. Choose a ${cpu.socket} motherboard.")
    }

    private fun memoryOutcome(cpu: HardwarePart?, board: HardwarePart?, ram: HardwarePart?) = when {
        cpu == null || board == null || ram == null -> pending("Memory type", "Choose a CPU, motherboard and RAM. We will check whether they use the same memory type.")
        cpu.supportedRam == ram.ramGeneration && board.supportedRam == ram.ramGeneration ->
            pass("Memory type", "The CPU, motherboard and RAM all use ${ram.ramGeneration}. This memory will fit.")
        else -> fail("Memory type", "This RAM is ${ram.ramGeneration}, but the selected CPU and motherboard need ${cpu.supportedRam} / ${board.supportedRam}. DDR4 and DDR5 use different slots, so they cannot be mixed.")
    }

    private fun caseOutcome(board: HardwarePart?, case: HardwarePart?) = when {
        board == null || case == null -> pending("Motherboard and case", "Choose a motherboard and case. We will check their physical sizes.")
        board.formFactor in case.supportedFormFactors -> pass("Motherboard and case", "This case has space for a ${board.formFactor} motherboard.")
        else -> fail("Motherboard and case", "This is a ${board.formFactor} motherboard, but the case is not made for that size. Choose a case that supports ${board.formFactor}.")
    }

    private fun gpuFitOutcome(gpu: HardwarePart?, case: HardwarePart?) = when {
        gpu == null || case == null -> pending("GPU and case", "Choose a graphics card and case. We will check whether the card is short enough to fit.")
        (gpu.gpuLengthMm ?: 0) <= (case.maxGpuLengthMm ?: 0) -> pass("GPU and case", "The graphics card is ${gpu.gpuLengthMm} mm long. The case allows up to ${case.maxGpuLengthMm} mm, so it fits.")
        else -> fail("GPU and case", "The graphics card is ${gpu.gpuLengthMm} mm long, but the case allows only ${case.maxGpuLengthMm} mm. Choose a shorter card or a larger case.")
    }

    private fun powerOutcome(draft: BuildDraft, psu: HardwarePart?): RuleOutcome {
        if (psu == null) return pending("Power supply", "Choose a power supply. We will check that it has enough extra power for the whole computer.")
        val required = ((draft.selections.values.sumOf(HardwarePart::power) + BASE_SYSTEM_POWER) * POWER_HEADROOM).toInt()
        return if ((psu.psuWattage ?: 0) >= required) {
            pass("Power supply", "The parts need about $required W after adding 25% spare power. This ${psu.psuWattage} W power supply leaves that safety room.")
        } else {
            fail("Power supply", "The parts need about $required W after adding spare power, but this power supply provides only ${psu.psuWattage} W. Choose a higher number of watts.")
        }
    }

    private fun budgetOutcome(draft: BuildDraft, mission: Mission) = if (draft.totalCost <= mission.budget) {
        pass("Budget", "Your build costs S$${draft.totalCost}, which is within this mission's S$${mission.budget} budget.")
    } else {
        fail("Budget", "Your build costs S$${draft.totalCost}. That is S$${draft.totalCost - mission.budget} over this mission's S$${mission.budget} budget. Try a lower-cost part.")
    }

    private fun missionSocketOutcome(cpu: HardwarePart?, board: HardwarePart?, requiredSocket: String) = when {
        cpu?.socket == requiredSocket && board?.socket == requiredSocket -> pass("Mission platform", "This task asks for the $requiredSocket platform, and both selected parts use it.")
        else -> fail("Mission platform", "This task asks for an $requiredSocket CPU and motherboard. Pick both parts with $requiredSocket in their details.")
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
        pass("Mission performance", "Your learning score is $score / 100. It meets this mission's target of ${mission.minimumPerformanceScore}.")
    } else {
        fail("Mission performance", "Your learning score is $score / 100, but this mission needs ${mission.minimumPerformanceScore}. Try improving the parts most important for this task.")
    }

    private fun pass(title: String, explanation: String) = RuleOutcome(title, OutcomeStatus.PASS, explanation)
    private fun fail(title: String, explanation: String) = RuleOutcome(title, OutcomeStatus.FAIL, explanation)
    private fun pending(title: String, explanation: String) = RuleOutcome(title, OutcomeStatus.WARNING, explanation)

    private companion object {
        const val BASE_SYSTEM_POWER = 120
        const val POWER_HEADROOM = 1.25
    }
}
