package com.musicflow.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.Black
import com.musicflow.app.ui.theme.OnBackground

@Composable
fun LyricsOverlay(
    lyrics: String?,
    currentPositionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    if (lyrics.isNullOrBlank()) return

    val lines = parseLyrics(lyrics)
    val currentLineIndex = remember(currentPositionMs, lines) {
        calculateCurrentLine(currentPositionMs, durationMs, lines)
    }

    val listState = rememberLazyListState()

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && lines.isNotEmpty()) {
            listState.animateScrollToItem(
                index = currentLineIndex,
                scrollOffset = -200
            )
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (lyrics.isNotBlank()) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .alpha(alpha)
            .background(Black.copy(alpha = 0.85f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (lines.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(lines) { index, line ->
                    val isCurrent = index == currentLineIndex
                    val textColor = if (isCurrent) AccentGreen else OnBackground.copy(alpha = 0.4f)
                    val fontSize = if (isCurrent) 18.sp else 14.sp
                    val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal

                    Text(
                        text = line.text,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = textColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }
            }
        } else {
            // Plain text fallback
            androidx.compose.material3.Text(
                text = lyrics,
                style = MaterialTheme.typography.bodyLarge,
                color = OnBackground.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )
        }
    }
}

private fun parseLyrics(lyrics: String): List<LyricsLine> {
    val lines = mutableListOf<LyricsLine>()
    val lrcPattern = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

    lyrics.lines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@forEach

        val match = lrcPattern.find(trimmed)
        if (match != null) {
            val minutes = match.groupValues[1].toInt()
            val seconds = match.groupValues[2].toInt()
            val fracStr = match.groupValues[3]
            val centiseconds = if (fracStr.length == 2) fracStr.toInt() * 10 else fracStr.toInt()
            val text = match.groupValues[4].trim()
            val timeMs = ((minutes * 60 + seconds) * 1000 + centiseconds).toLong()
            lines.add(LyricsLine(timeMs, text))
        } else {
            lines.add(LyricsLine(-1L, trimmed))
        }
    }
    return lines
}

private fun calculateCurrentLine(
    positionMs: Long,
    durationMs: Long,
    lines: List<LyricsLine>,
): Int {
    if (lines.isEmpty() || durationMs <= 0) return 0
    var currentIndex = 0
    var bestDiff = Long.MAX_VALUE
    for (i in lines.indices) {
        val line = lines[i]
        if (line.timeMs >= 0 && line.timeMs <= positionMs) {
            val diff = positionMs - line.timeMs
            if (diff < bestDiff) {
                bestDiff = diff
                currentIndex = i
            }
        }
    }
    return currentIndex.coerceAtMost(lines.lastIndex)
}

@Composable
private fun Text(text: String, fontSize: androidx.compose.ui.unit.TextUnit, fontWeight: FontWeight, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier,
    )
}

private data class LyricsLine(
    val timeMs: Long,
    val text: String,
)