package com.musicflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.Black
import com.musicflow.app.ui.theme.DarkSurface
import com.musicflow.app.ui.theme.DarkSurfaceVariant
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant
import com.musicflow.app.utils.SearchLanguage

/**
 * Onboarding screen shown on first app launch.
 * Asks the user to select their preferred languages for search results.
 * Supports selecting multiple languages.
 */
@Composable
fun OnboardingScreen(
    onLanguageSelected: (List<SearchLanguage>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedLanguages = remember { mutableStateListOf(SearchLanguage.ENGLISH) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Icon(
            imageVector = Icons.Filled.Language,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.size(72.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Infinity Stream",
            style = MaterialTheme.typography.headlineMedium,
            color = OnBackground,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Choose your preferred languages\n(you can select multiple)",
            style = MaterialTheme.typography.bodyLarge,
            color = OnBackgroundVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${selectedLanguages.size} selected",
            style = MaterialTheme.typography.bodySmall,
            color = AccentGreen,
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(SearchLanguage.entries) { language ->
                val isSelected = language in selectedLanguages
                val bgColor = if (isSelected) AccentGreen.copy(alpha = 0.2f) else DarkSurfaceVariant

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable {
                            if (isSelected) {
                                if (language != SearchLanguage.ENGLISH) {
                                    selectedLanguages.remove(language)
                                }
                            } else {
                                selectedLanguages.add(language)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Box(modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = language.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) AccentGreen else OnBackground,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        Button(
            onClick = { onLanguageSelected(selectedLanguages.toList()) },
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGreen,
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = selectedLanguages.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(56.dp),
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Black,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}