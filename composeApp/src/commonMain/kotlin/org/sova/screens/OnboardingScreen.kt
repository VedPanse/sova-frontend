package org.sova.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.sova.components.ChipInput
import org.sova.components.HealthCard
import org.sova.components.HealthDateField
import org.sova.components.HealthSegmentedSelector
import org.sova.components.HealthTextField
import org.sova.components.PrimaryButton
import org.sova.components.SecondaryButton
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.logic.ProfileValidation
import org.sova.model.MedicalProfile
import org.sova.model.UserProfile

@Composable
fun OnboardingScreen(
    patientId: String,
    onComplete: (UserProfile, MedicalProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(0) }
    var dobDigits by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var surgery by remember { mutableStateOf("") }
    var dischargeDigits by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var doctorPhoneNumber by remember { mutableStateOf("") }

    val steps = listOf(
        WizardStep("Birth and sex", "This helps compare signals more carefully."),
        WizardStep("Recovery context", "A few details for caregiver handoffs."),
        WizardStep("Medical notes", "Details your care team may need."),
        WizardStep("Care contacts", "Who should be easy to reach."),
    )

    val dob = formatDateOfBirth(dobDigits)
    val dobError = visibleError(dobDigits) { ProfileValidation.dobError(dob) }
    val sexError: String? = null
    val dischargeDate = formatDateOfBirth(dischargeDigits)
    val surgeryError = visibleError(surgery) { ProfileValidation.requiredError(it, "Surgery") }
    val dischargeDateError = visibleError(dischargeDigits) { ProfileValidation.dateError(dischargeDate, "Discharge date") }
    val emergencyNameError = visibleError(emergencyName) { ProfileValidation.requiredError(it, "Emergency contact name") }
    val emergencyPhoneError = visibleError(emergencyPhone) { ProfileValidation.phoneError(it, required = true) }
    val doctorPhoneError = visibleError(doctorPhoneNumber) { ProfileValidation.phoneError(it, required = false) }

    val currentStepValid = when (step) {
        0 -> ProfileValidation.dobError(dob) == null && sex.isNotBlank()
        1 -> ProfileValidation.requiredError(surgery, "Surgery") == null &&
            ProfileValidation.dateError(dischargeDate, "Discharge date") == null
        2 -> true
        else -> ProfileValidation.requiredError(emergencyName, "Emergency contact name") == null &&
            ProfileValidation.phoneError(emergencyPhone, required = true) == null &&
            ProfileValidation.phoneError(doctorPhoneNumber, required = false) == null
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                Text(steps[step].title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.headlineLarge)
                Text(steps[step].subtitle, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                LinearProgressIndicator(
                    progress = { (step + 1).toFloat() / steps.size.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = HealthColors.Accent,
                    trackColor = HealthColors.SurfaceSubtle,
                    gapSize = HealthSpacing.None,
                    drawStopIndicator = {},
                )
                Text("Step ${step + 1} of ${steps.size}", color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
        item {
            HealthCard {
                Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                    when (step) {
                        0 -> {
                            HealthDateField(
                                label = "Date of birth",
                                digits = dobDigits,
                                onDigitsChange = { dobDigits = it },
                                helperText = "Use MM/DD/YYYY.",
                                errorText = dobError,
                            )
                            HealthSegmentedSelector(
                                label = "Sex",
                                options = listOf("Female", "Male", "Other", "Prefer not to say"),
                                selected = sex,
                                onSelected = { sex = it },
                                helperText = "Choose the option that best fits.",
                                errorText = sexError,
                            )
                        }
                        1 -> {
                            HealthTextField("Surgery", surgery, { surgery = it }, helperText = "Like appendectomy or knee repair.", errorText = surgeryError)
                            HealthDateField(
                                label = "Discharge date",
                                digits = dischargeDigits,
                                onDigitsChange = { dischargeDigits = it },
                                helperText = "Use MM/DD/YYYY.",
                                errorText = dischargeDateError,
                            )
                            HealthTextField("Address", address, { address = it }, helperText = "Optional. Used for care coordination context.")
                        }
                        2 -> {
                            ChipInput(
                                label = "Current medications",
                                value = medications,
                                onValueChange = { medications = it },
                                helperText = "Optional. Separate with commas.",
                                chips = ProfileValidation.splitList(medications),
                            )
                            ChipInput(
                                label = "Allergies",
                                value = allergies,
                                onValueChange = { allergies = it },
                                helperText = "Optional. Separate with commas.",
                                chips = ProfileValidation.splitList(allergies),
                            )
                        }
                        else -> {
                            HealthTextField("Emergency contact name", emergencyName, { emergencyName = it }, errorText = emergencyNameError)
                            HealthTextField("Emergency contact phone", emergencyPhone, { emergencyPhone = it }, errorText = emergencyPhoneError, keyboardType = KeyboardType.Phone)
                            HealthTextField("Care team phone", doctorPhoneNumber, { doctorPhoneNumber = it }, helperText = "Optional phone number.", errorText = doctorPhoneError, keyboardType = KeyboardType.Phone)
                        }
                    }
                }
            }
        }
        item {
            PrimaryButton(
                text = if (step == steps.lastIndex) "Start monitoring" else "Continue",
                enabled = currentStepValid,
                onClick = {
                    if (step == steps.lastIndex) {
                        onComplete(
                            UserProfile(
                                patientId = patientId,
                                firstName = "Patient",
                                lastName = "",
                                dob = dob.trim(),
                                sex = sex,
                                address = address.trim().ifBlank { null },
                                heightFeet = 0,
                                heightInches = 0,
                                weightPounds = 0,
                                surgery = surgery.trim().ifBlank { null },
                                dischargeDate = dischargeDate.trim().ifBlank { null },
                                emergencyContactName = emergencyName.trim(),
                                emergencyContactPhone = emergencyPhone.trim(),
                                doctorPhoneNumber = doctorPhoneNumber.trim().ifBlank { null },
                            ),
                            MedicalProfile(
                                conditions = emptyList(),
                                medications = ProfileValidation.splitList(medications),
                                allergies = ProfileValidation.splitList(allergies),
                            ),
                        )
                    } else {
                        step += 1
                    }
                },
            )
        }
        if (step > 0) {
            item {
                SecondaryButton("Back", onClick = { step -= 1 })
            }
        }
    }
}

private data class WizardStep(
    val title: String,
    val subtitle: String,
)

private fun visibleError(value: String, validator: (String) -> String?): String? =
    if (value.isBlank()) null else validator(value)

private fun formatDateOfBirth(value: String): String {
    val digits = value.filter { it.isDigit() }.take(8)
    return buildString {
        digits.forEachIndexed { index, char ->
            if (index == 2 || index == 4) append("/")
            append(char)
        }
    }
}
