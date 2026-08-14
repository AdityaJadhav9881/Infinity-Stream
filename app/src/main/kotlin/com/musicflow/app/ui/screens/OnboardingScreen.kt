package com.musicflow.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.musicflow.app.ui.theme.MFColors
import com.musicflow.app.ui.theme.MFGlass
import com.musicflow.app.ui.theme.MFTokens
import com.musicflow.app.utils.SearchLanguage

private val HOW_DID_YOU_HEAR_SUGGESTIONS = listOf(
    "Friend or family",
    "YouTube",
    "Reddit",
    "Twitter / X",
    "Instagram / TikTok",
    "Google Search",
    "F-Droid / GitHub",
    "Other"
)

@Composable
fun OnboardingScreen(
    onboardingData: OnboardingResult,
    modifier: Modifier = Modifier,
) {
    val selectedLanguages = remember { mutableStateListOf(SearchLanguage.ENGLISH) }
    var fullName by remember { mutableStateOf("") }
    var howDidYouHear by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }
    val isStep0Valid = fullName.isNotBlank() && howDidYouHear.isNotBlank()

    val filteredSuggestions = HOW_DID_YOU_HEAR_SUGGESTIONS.filter {
        it.contains(howDidYouHear, ignoreCase = true) && howDidYouHear.isNotBlank()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MFColors.Background)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        MFGlass.GlassPanel(
            modifier = Modifier.size(88.dp),
            cornerRadius = RoundedCornerShape(28.dp),
            alpha = 0.10f,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = MFColors.Accent,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Infinity Stream",
            style = MaterialTheme.typography.headlineMedium,
            color = MFColors.TextPrimary,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedVisibility(visible = step == 0, enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tell us a bit about yourself",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MFColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(36.dp))

                MFGlass.GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = RoundedCornerShape(18.dp),
                    alpha = 0.08f,
                ) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Your name", color = MFColors.TextTertiary) },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = MFColors.TextTertiary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MFColors.TextPrimary,
                            unfocusedTextColor = MFColors.TextPrimary,
                            cursorColor = MFColors.Accent,
                            focusedBorderColor = MFColors.Accent,
                            unfocusedBorderColor = MFColors.GlassBorder,
                            focusedLabelColor = MFColors.Accent,
                            unfocusedLabelColor = MFColors.TextTertiary,
                            focusedLeadingIconColor = MFColors.Accent,
                            unfocusedLeadingIconColor = MFColors.TextTertiary,
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    MFGlass.GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = RoundedCornerShape(18.dp),
                        alpha = 0.08f,
                    ) {
                        OutlinedTextField(
                            value = howDidYouHear,
                            onValueChange = {
                                howDidYouHear = it
                                showSuggestions = it.isNotBlank()
                            },
                            label = { Text("How did you hear about us?", color = MFColors.TextTertiary) },
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = MFColors.TextTertiary)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MFColors.TextPrimary,
                                unfocusedTextColor = MFColors.TextPrimary,
                                cursorColor = MFColors.Accent,
                                focusedBorderColor = MFColors.Accent,
                                unfocusedBorderColor = MFColors.GlassBorder,
                                focusedLabelColor = MFColors.Accent,
                                unfocusedLabelColor = MFColors.TextTertiary,
                                focusedLeadingIconColor = MFColors.Accent,
                                unfocusedLeadingIconColor = MFColors.TextTertiary,
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    DropdownMenu(
                        expanded = showSuggestions && filteredSuggestions.isNotEmpty(),
                        onDismissRequest = { showSuggestions = false },
                        modifier = Modifier
                            .background(MFColors.GlassLow)
                            .border(width = 0.5.dp, color = MFColors.GlassBorder, shape = RoundedCornerShape(12.dp)),
                    ) {
                        filteredSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = suggestion,
                                        color = MFColors.TextPrimary,
                                    )
                                },
                                onClick = {
                                    howDidYouHear = suggestion
                                    showSuggestions = false
                                },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = step == 1, enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Choose your preferred languages\n(you can select multiple)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MFColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${selectedLanguages.size} selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MFColors.Accent,
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        if (step == 0 && !isStep0Valid) {
            Text(
                text = "Please fill in all fields to continue",
                style = MaterialTheme.typography.bodySmall,
                color = MFColors.Error,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (step == 1) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(SearchLanguage.entries) { language ->
                    val isSelected = language in selectedLanguages
                    val bgColor = if (isSelected) MFColors.Accent.copy(alpha = 0.18f) else MFColors.GlassLow
                    val borderColor = if (isSelected) MFColors.Accent.copy(alpha = 0.4f) else MFColors.GlassBorder

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MFTokens.MediumRadius)
                            .background(bgColor)
                            .border(width = 0.5.dp, color = borderColor, shape = MFTokens.MediumRadius)
                            .clickable {
                                if (isSelected) {
                                    if (language != SearchLanguage.ENGLISH) {
                                        selectedLanguages.remove(language)
                                    }
                                } else {
                                    selectedLanguages.add(language)
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MFColors.Accent,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Box(modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = language.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MFColors.Accent else MFColors.TextPrimary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Button(
            onClick = {
                if (step == 0) {
                    step = 1
                } else {
                    onboardingData.onResult(
                        fullName.trim(),
                        howDidYouHear.trim(),
                        selectedLanguages.toList()
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MFColors.Accent,
            ),
            shape = MFTokens.MediumRadius,
            enabled = if (step == 0) isStep0Valid else selectedLanguages.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp)
                .height(56.dp),
        ) {
            Text(
                text = if (step == 0) "Next" else "Continue",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MFColors.TextOnAccent,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

data class OnboardingResult(
    val onResult: (fullName: String, howDidYouHear: String, languages: List<SearchLanguage>) -> Unit
)
