package com.musicflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.musicflow.app.ui.theme.MFColors

@Composable
fun UnauthorizedScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MFColors.Background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Block,
            contentDescription = null,
            tint = MFColors.Accent.copy(alpha = 0.6f),
            modifier = Modifier.size(96.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Access Revoked",
            style = MaterialTheme.typography.headlineMedium,
            color = MFColors.TextPrimary,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This device has been disabled by an administrator.\n\nPlease contact support for assistance.",
            style = MaterialTheme.typography.bodyLarge,
            color = MFColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
