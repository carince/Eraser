package dev.carince.eraser.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.carince.eraser.ui.DueListScreen
import java.time.ZoneId

@Composable
fun EraserApp(
    state: FeedUiState,
    zone: ZoneId,
    onSaveUrl: (String) -> Unit,
    onRetry: () -> Unit,
    onEditFeed: () -> Unit,
) {
    when (state) {
        is FeedUiState.Setup -> FeedSetupScreen(
            initialUrl = state.url,
            error = state.error,
            onSave = onSaveUrl,
        )

        FeedUiState.Loading -> LoadingScreen()

        is FeedUiState.Content -> DueListScreen(
            items = state.items,
            now = state.now,
            zone = zone,
            refreshing = state.refreshing,
            warning = state.warning,
            onRefresh = onRetry,
            onEditFeed = onEditFeed,
        )

        is FeedUiState.Error -> FeedErrorScreen(
            message = state.message,
            onRetry = onRetry,
            onEditFeed = onEditFeed,
        )
    }
}

@Composable
private fun FeedSetupScreen(
    initialUrl: String?,
    error: String?,
    onSave: (String) -> Unit,
) {
    var url by remember(initialUrl) { mutableStateOf(initialUrl.orEmpty()) }

    Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Text(
                text = "Eraser",
                fontSize = 34.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.8).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Paste your Blackboard calendar link to pull in every deadline.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Calendar link") },
                placeholder = { Text("https://…/calendar.ics") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(url) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (initialUrl == null) "Save link" else "Update link")
            }

            Spacer(Modifier.height(32.dp))
            Text(
                text = "Where to find it",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Blackboard → Calendar → Get External Calendar Link",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "The link is a private token. It stays on this device.",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Loading your calendar…",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeedErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onEditFeed: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Couldn't load your calendar",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.4).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onRetry) {
                    Text("Try again")
                }
                Spacer(Modifier.padding(horizontal = 4.dp))
                TextButton(onClick = onEditFeed) {
                    Text("Edit link")
                }
            }
        }
    }
}
