package org.sova.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sova.components.HealthCard
import org.sova.components.PrimaryButton
import org.sova.components.SectionHeader
import org.sova.components.StatusPill
import org.sova.design.HealthColors
import org.sova.design.HealthSpacing
import org.sova.model.SimulationResult
import org.sova.model.UserProfile

@Composable
fun SimulationScreen(
    user: UserProfile,
    hasRun: Boolean,
    result: SimulationResult,
    onRunSimulation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                Text("Run simulation", color = HealthColors.TextPrimary, style = MaterialTheme.typography.headlineLarge)
                Text("Sova uses ${user.firstName}'s profile and today's signals to estimate the next 6 hours.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item { PrimaryButton("Run simulation", onRunSimulation) }
        if (hasRun) {
            item {
                HealthCard {
                    Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                        SectionHeader("Result", result.summary)
                        StatusPill("${result.riskLevel.name} risk", result.riskLevel)
                    }
                }
            }
            item {
                HealthCard {
                    Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                        SectionHeader("Why")
                        result.reasons.forEach {
                            Text(it, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
