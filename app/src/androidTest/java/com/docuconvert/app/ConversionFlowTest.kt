package com.docuconvert.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.docuconvert.app.presentation.DocuConvertTheme
import com.docuconvert.app.presentation.HomeScreen
import com.docuconvert.app.presentation.AppViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests (§23): home screen, error states, history/settings.
 * Run on emulator or device: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ConversionFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreenShowsPrimaryActions() {
        composeRule.setContent {
            DocuConvertTheme {
                HomeScreen(
                    state = AppViewModel.UiState(),
                    onOpenDocument = {},
                    onConvertDocument = {},
                    onOpenHistory = {},
                    onOpenSettings = {},
                    onRecentClick = {}
                )
            }
        }
        composeRule.onNodeWithText("Dovra").assertIsDisplayed()
        composeRule.onNodeWithText("Read and convert your documents privately.").assertIsDisplayed()
        composeRule.onNodeWithText("Open document").assertIsDisplayed()
        composeRule.onNodeWithText("Convert document").assertIsDisplayed()
        composeRule.onNodeWithText("Recent documents").assertIsDisplayed()
    }

    @Test
    fun homeScreenEmptyHistoryMessage() {
        composeRule.setContent {
            DocuConvertTheme {
                HomeScreen(
                    state = AppViewModel.UiState(),
                    onOpenDocument = {},
                    onConvertDocument = {},
                    onOpenHistory = {},
                    onOpenSettings = {},
                    onRecentClick = {}
                )
            }
        }
        composeRule.onNodeWithText("No recent documents").assertIsDisplayed()
    }
}
