package com.sabriusta.tv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sabriusta.tv.ui.components.EmptyState
import com.sabriusta.tv.ui.components.ErrorBox
import com.sabriusta.tv.ui.components.MediaRowItem
import com.sabriusta.tv.ui.theme.SabriUstaTvTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentsUiTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun bosDurumMesajiVeButonuGorunur() {
        var clicked = false
        composeRule.setContent {
            SabriUstaTvTheme {
                EmptyState(
                    title = "Henuz yayin yok",
                    description = "M3U listesi ekleyin.",
                    actionLabel = "M3U listesi ekle",
                    onAction = { clicked = true }
                )
            }
        }
        composeRule.onNodeWithText("Henuz yayin yok").assertIsDisplayed()
        composeRule.onNodeWithText("M3U listesi ekle").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun hataKutusuYenidenDeneDugmesiCalisir() {
        var retried = false
        composeRule.setContent {
            SabriUstaTvTheme {
                ErrorBox(message = "Sunucu yanit vermiyor.", onRetry = { retried = true })
            }
        }
        composeRule.onNodeWithText("Sunucu yanit vermiyor.").assertIsDisplayed()
        composeRule.onNodeWithText("Yeniden dene").performClick()
        assertThat(retried).isTrue()
    }

    @Test
    fun kanalSatiriTiklamaVeFavoriCalisir() {
        var opened = false
        var favorited = false
        composeRule.setContent {
            SabriUstaTvTheme {
                MediaRowItem(
                    title = "Kanal A",
                    subtitle = "Ulusal",
                    logoUrl = null,
                    isFavorite = false,
                    onFavoriteClick = { favorited = true },
                    onClick = { opened = true }
                )
            }
        }
        composeRule.onNodeWithText("Kanal A").assertIsDisplayed()
        composeRule.onNodeWithText("Ulusal").assertIsDisplayed()
        composeRule.onNodeWithText("Kanal A").performClick()
        assertThat(opened).isTrue()
        assertThat(favorited).isFalse()
    }
}
