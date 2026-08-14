package com.musicflow.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.musicflow.app.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scaleAnimatable = remember { Animatable(0.72f) }
    val alphaAnimatable = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                scaleAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 650,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
            launch {
                alphaAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 550,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
            launch {
                glowAlpha.animateTo(
                    targetValue = 0.6f,
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
        }

        delay(150)

        glowAlpha.animateTo(
            targetValue = 0.2f,
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing,
            ),
        )

        delay(50)

        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = "MusicFlow",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    scaleX = scaleAnimatable.value
                    scaleY = scaleAnimatable.value
                    alpha = alphaAnimatable.value
                }
                .drawBehind {
                    val glowRadius = 200f * scaleAnimatable.value
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1ED760).copy(alpha = glowAlpha.value * 0.35f),
                                Color(0xFF22D3EE).copy(alpha = glowAlpha.value * 0.15f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = glowRadius * density,
                        ),
                        radius = glowRadius * density,
                        center = Offset(size.width / 2, size.height / 2),
                    )
                },
        )
    }
}
