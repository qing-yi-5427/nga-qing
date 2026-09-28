package com.qingyi5427.ngaqing.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Common sizes for native touch surfaces; independent of device form factor. */
object NgaDimensions {
    val pagePadding = 16.dp
    val itemSpacing = 12.dp
    val minimumTouch = 48.dp
    val readingWidth = 760.dp
}

@Composable
fun NgaOutlinedCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Empty and error feedback stays reachable in short windows and with large text. */
@Composable
fun NgaStatePanel(
    title: String,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(NgaDimensions.pagePadding),
        contentAlignment = Alignment.Center
    ) {
        NgaOutlinedCard(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NgaDimensions.itemSpacing)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                if (!description.isNullOrBlank()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                if (actionLabel != null && onAction != null) {
                    Button(onClick = onAction, modifier = Modifier.heightIn(min = NgaDimensions.minimumTouch)) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}
