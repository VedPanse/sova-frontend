package org.sova.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HealthSpacing.Xl + HealthSpacing.Sm)
            .pointerHoverIcon(PointerIcon.Hand),
        shape = HealthShapes.Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = HealthColors.Accent,
            contentColor = HealthColors.Surface,
            disabledContainerColor = HealthColors.SurfaceSubtle,
            disabledContentColor = HealthColors.TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = HealthSpacing.Md, vertical = HealthSpacing.Sm),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = HealthSpacing.None),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
