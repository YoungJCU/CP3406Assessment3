package com.youngjcu.pclab.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youngjcu.pclab.data.repository.LearningStatistics
import com.youngjcu.pclab.domain.model.HardwareCatalogue
import com.youngjcu.pclab.domain.model.Mission
import com.youngjcu.pclab.domain.model.PerformanceWeights
import com.youngjcu.pclab.ui.screens.HomeScreen
import com.youngjcu.pclab.ui.theme.PcLabTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun startMissionOpensTheSelectedMission() {
        var startedMissionId: Int? = null
        val mission = Mission(
            id = 7,
            title = "Programming workstation",
            description = "Build a development PC.",
            budget = 1500,
            requiredSocket = "AM5",
            minimumPerformanceScore = 75,
            performanceWeights = PerformanceWeights(0.35, 0.10, 0.30, 0.25),
            requirements = emptyList(),
            hint = "Start with the platform."
        )

        composeRule.setContent {
            PcLabTheme(darkTheme = false, colourBlindMode = false) {
                HomeScreen(
                    catalogue = HardwareCatalogue(emptyMap(), listOf(mission)),
                    statistics = LearningStatistics(),
                    onStartMission = { startedMissionId = it },
                    onStatistics = {},
                    onSettings = {}
                )
            }
        }

        composeRule.onNodeWithText("Programming workstation").assertIsDisplayed()
        composeRule.onNodeWithText("Start mission").performClick()
        assertEquals(7, startedMissionId)
    }
}
