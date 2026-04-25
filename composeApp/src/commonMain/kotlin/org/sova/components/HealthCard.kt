package org.sova.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing

@Composable
fun HealthCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(HealthSpacing.Md),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = HealthShapes.Card,
        colors = CardDefaults.cardColors(containerColor = HealthColors.Surface),
        border = BorderStroke(width = HealthSpacing.Stroke, color = HealthColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = HealthSpacing.None),
    ) {
        Column(
            modifier = Modifier
                .background(HealthColors.Surface)
                .padding(contentPadding),
            content = content,
        )
    }
}
