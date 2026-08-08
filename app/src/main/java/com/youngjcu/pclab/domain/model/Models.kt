package com.youngjcu.pclab.domain.model

enum class PartCategory(val label: String) {
    CPU("CPU"),
    MOTHERBOARD("Motherboard"),
    GPU("GPU"),
    RAM("RAM"),
    STORAGE("Storage"),
    PSU("Power supply"),
    CASE("Case")
}

data class HardwarePart(
    val id: Int,
    val category: PartCategory,
    val name: String,
    val brand: String,
    val price: Int,
    val power: Int,
    val performanceScore: Int,
    val learningNote: String,
    val socket: String? = null,
    val supportedRam: String? = null,
    val ramGeneration: String? = null,
    val ramCapacityGb: Int? = null,
    val formFactor: String? = null,
    val supportedFormFactors: List<String> = emptyList(),
    val maxGpuLengthMm: Int? = null,
    val gpuLengthMm: Int? = null,
    val psuWattage: Int? = null,
    val storageCapacityGb: Int? = null,
    val recommendedFor: List<String> = emptyList()
)

data class Mission(
    val id: Int,
    val title: String,
    val description: String,
    val budget: Int,
    val requiredSocket: String?,
    val minimumPerformanceScore: Int,
    val performanceWeights: PerformanceWeights,
    val requirements: List<String>,
    val hint: String
)

data class PerformanceWeights(
    val cpu: Double,
    val gpu: Double,
    val ram: Double,
    val storage: Double
)

data class HardwareCatalogue(
    val parts: Map<PartCategory, List<HardwarePart>>,
    val missions: List<Mission>
)

data class BuildDraft(
    val selections: Map<PartCategory, HardwarePart> = emptyMap()
) {
    fun part(category: PartCategory): HardwarePart? = selections[category]

    fun withPart(part: HardwarePart): BuildDraft = copy(selections = selections + (part.category to part))

    val isComplete: Boolean get() = PartCategory.entries.all { it in selections }

    val totalCost: Int get() = selections.values.sumOf(HardwarePart::price)
}

enum class OutcomeStatus { PASS, WARNING, FAIL }

data class RuleOutcome(
    val title: String,
    val status: OutcomeStatus,
    val explanation: String
)

data class Evaluation(
    val outcomes: List<RuleOutcome>,
    val totalCost: Int,
    val estimatedPower: Int,
    val performanceScore: Int,
    val score: Int
) {
    val isCompatible: Boolean
        get() = outcomes
            .filterNot { it.title == "Budget" || it.title == "Mission performance" }
            .none { it.status == OutcomeStatus.FAIL }
    val isWithinBudget: Boolean get() = outcomes.firstOrNull { it.title == "Budget" }?.status == OutcomeStatus.PASS
}
