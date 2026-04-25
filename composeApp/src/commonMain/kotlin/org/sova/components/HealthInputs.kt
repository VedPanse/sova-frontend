package org.sova.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing

@Composable
fun HealthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HealthSpacing.Xl + HealthSpacing.Lg),
            label = { Text(label) },
            shape = HealthShapes.SmallCard,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            isError = errorText != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HealthColors.Accent,
                unfocusedBorderColor = HealthColors.Border,
                errorBorderColor = HealthColors.Danger,
                focusedContainerColor = HealthColors.Surface,
                unfocusedContainerColor = HealthColors.Surface,
                errorContainerColor = HealthColors.Surface,
                focusedTextColor = HealthColors.TextPrimary,
                unfocusedTextColor = HealthColors.TextPrimary,
                focusedLabelColor = HealthColors.TextSecondary,
                unfocusedLabelColor = HealthColors.TextSecondary,
            ),
        )
        FieldNote(helperText = helperText, errorText = errorText)
    }
}

@Composable
fun HealthNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    errorText: String? = null,
) {
    HealthTextField(
        label = label,
        value = value,
        onValueChange = { candidate -> onValueChange(candidate.filter { it.isDigit() }) },
        modifier = modifier,
        helperText = helperText,
        errorText = errorText,
        keyboardType = KeyboardType.Number,
    )
}

@Composable
fun HealthDateField(
    label: String,
    digits: String,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    errorText: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
    ) {
        OutlinedTextField(
            value = digits,
            onValueChange = { candidate ->
                onDigitsChange(candidate.filter { it.isDigit() }.take(8))
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HealthSpacing.Xl + HealthSpacing.Lg),
            label = { Text(label) },
            shape = HealthShapes.SmallCard,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = errorText != null,
            visualTransformation = DateSlashTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HealthColors.Accent,
                unfocusedBorderColor = HealthColors.Border,
                errorBorderColor = HealthColors.Danger,
                focusedContainerColor = HealthColors.Surface,
                unfocusedContainerColor = HealthColors.Surface,
                errorContainerColor = HealthColors.Surface,
                focusedTextColor = HealthColors.TextPrimary,
                unfocusedTextColor = HealthColors.TextPrimary,
                focusedLabelColor = HealthColors.TextSecondary,
                unfocusedLabelColor = HealthColors.TextSecondary,
            ),
        )
        FieldNote(helperText = helperText, errorText = errorText)
    }
}

@Composable
fun HealthSegmentedSelector(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    errorText: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
    ) {
        Text(label, color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HealthColors.SurfaceSubtle, HealthShapes.Pill)
                .padding(HealthSpacing.Xs),
            horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = HealthSpacing.Xl + HealthSpacing.Sm)
                        .clip(HealthShapes.Pill)
                        .clickable { onSelected(option) },
                    shape = HealthShapes.Pill,
                    color = if (isSelected) HealthColors.Surface else HealthColors.SurfaceSubtle,
                    border = if (isSelected) BorderStroke(HealthSpacing.Stroke, HealthColors.Border) else null,
                    shadowElevation = HealthSpacing.None,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = HealthSpacing.Xl + HealthSpacing.Sm)
                            .padding(horizontal = HealthSpacing.Xs),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option,
                            color = if (isSelected) HealthColors.TextPrimary else HealthColors.TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
        FieldNote(helperText = helperText, errorText = errorText)
    }
}

private object DateSlashTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(8)
        val formatted = buildString {
            append(digits.take(2))
            if (digits.length >= 2) append("/")
            append(digits.drop(2).take(2))
            if (digits.length >= 4) append("/")
            append(digits.drop(4))
        }
        return TransformedText(
            text = AnnotatedString(formatted),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    val safeOffset = offset.coerceIn(0, digits.length)
                    return when {
                        safeOffset <= 1 -> safeOffset
                        safeOffset <= 3 -> safeOffset + 1
                        else -> safeOffset + 2
                    }.coerceAtMost(formatted.length)
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return when {
                        offset <= 1 -> offset
                        offset <= 3 -> 2
                        offset <= 4 -> offset - 1
                        offset <= 6 -> 4
                        else -> offset - 2
                    }.coerceIn(0, digits.length)
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    chips: List<String> = emptyList(),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
    ) {
        HealthTextField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            helperText = helperText,
        )
        if (chips.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
                verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
            ) {
                chips.forEach { chip ->
                    Text(
                        text = chip,
                        modifier = Modifier
                            .background(HealthColors.SurfaceSubtle, HealthShapes.Pill)
                            .padding(horizontal = HealthSpacing.Sm, vertical = HealthSpacing.Xs),
                        color = HealthColors.TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldNote(
    helperText: String?,
    errorText: String?,
) {
    val text = errorText ?: helperText
    if (text != null) {
        Text(
            text = text,
            color = if (errorText != null) HealthColors.Danger else HealthColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
