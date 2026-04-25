package org.sova.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HealthSpacing.Xl + HealthSpacing.Sm)
            .pointerHoverIcon(PointerIcon.Hand),
        shape = HealthShapes.Pill,
        border = BorderStroke(HealthSpacing.Stroke, HealthColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = HealthColors.Surface,
            contentColor = HealthColors.TextPrimary,
            disabledContainerColor = HealthColors.Surface,
            disabledContentColor = HealthColors.TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = HealthSpacing.Md, vertical = HealthSpacing.Sm),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
