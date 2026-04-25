package org.sova.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import org.sova.components.HealthNumberField
import org.sova.components.HealthSegmentedSelector
import org.sova.components.HealthTextField
import org.sova.components.PrimaryButton
import org.sova.components.SecondaryButton
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.logic.PatientIdGenerator
import org.sova.logic.ProfileValidation
import org.sova.model.MedicalProfile
import org.sova.model.UserProfile

@Composable
fun OnboardingScreen(
    onComplete: (UserProfile, MedicalProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(0) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dobDigits by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var feet by remember { mutableStateOf("") }
    var inches by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var conditions by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var surgery by remember { mutableStateOf("") }
    var dischargeDigits by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var caregiverName by remember { mutableStateOf("") }
    var caregiverContact by remember { mutableStateOf("") }

    val steps = listOf(
        WizardStep("Your name", "Tell Sova who it is monitoring."),
        WizardStep("Birth and sex", "This helps compare signals more carefully."),
        WizardStep("Body", "A simple baseline for health context."),
        WizardStep("Medical notes", "Optional details your care team may need."),
        WizardStep("Recovery context", "A few details for caregiver handoffs."),
        WizardStep("Care contacts", "Who should be easy to reach."),
    )

    val firstNameError = visibleError(firstName) { ProfileValidation.nameError(it, "First name") }
    val lastNameError = visibleError(lastName) { ProfileValidation.nameError(it, "Last name") }
    val dob = formatDateOfBirth(dobDigits)
    val dobError = visibleError(dobDigits) { ProfileValidation.dobError(dob) }
    val sexError: String? = null
    val feetError = visibleError(feet) { ProfileValidation.numberRangeError(it, "Feet", 1, 8) }
    val inchesError = visibleError(inches) { ProfileValidation.numberRangeError(it, "Inches", 0, 11) }
    val weightError = visibleError(weight) { ProfileValidation.numberRangeError(it, "Weight", 20, 700) }
    val dischargeDate = formatDateOfBirth(dischargeDigits)
    val dischargeDateError = visibleError(dischargeDigits) { ProfileValidation.optionalDateError(dischargeDate, "discharge") }
    val emergencyNameError = visibleError(emergencyName) { ProfileValidation.requiredError(it, "Emergency contact name") }
    val emergencyPhoneError = visibleError(emergencyPhone) { ProfileValidation.phoneError(it, required = true) }
    val caregiverContactError = visibleError(caregiverContact) { ProfileValidation.phoneError(it, required = false) }

    val currentStepValid = when (step) {
        0 -> ProfileValidation.nameError(firstName, "First name") == null &&
            ProfileValidation.nameError(lastName, "Last name") == null
        1 -> ProfileValidation.dobError(dob) == null && sex.isNotBlank()
        2 -> ProfileValidation.numberRangeError(feet, "Feet", 1, 8) == null &&
            ProfileValidation.numberRangeError(inches, "Inches", 0, 11) == null &&
            ProfileValidation.numberRangeError(weight, "Weight", 20, 700) == null
        3 -> true
        4 -> ProfileValidation.optionalDateError(dischargeDate, "discharge") == null
        else -> ProfileValidation.requiredError(emergencyName, "Emergency contact name") == null &&
            ProfileValidation.phoneError(emergencyPhone, required = true) == null &&
            ProfileValidation.phoneError(caregiverContact, required = false) == null
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
                            HealthTextField("First name", firstName, { firstName = it }, helperText = "At least 2 letters.", errorText = firstNameError)
                            HealthTextField("Last name", lastName, { lastName = it }, helperText = "At least 2 letters.", errorText = lastNameError)
                        }
                        1 -> {
                            HealthDateField(
                                label = "Date of birth",
                                digits = dobDigits,
                                onDigitsChange = { dobDigits = it },
                                helperText = "Use MM/DD/YYYY.",
                                errorText = dobError,
                            )
                            HealthSegmentedSelector(
                                label = "Sex",
                                options = listOf("Female", "Male", "Other"),
                                selected = sex,
                                onSelected = { sex = it },
                                helperText = "Choose the option that best fits.",
                                errorText = sexError,
                            )
                        }
                        2 -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                                HealthNumberField("Feet", feet, { feet = it }, Modifier.weight(1f), errorText = feetError)
                                HealthNumberField("Inches", inches, { inches = it }, Modifier.weight(1f), errorText = inchesError)
                            }
                            HealthNumberField("Weight", weight, { weight = it }, helperText = "Pounds.", errorText = weightError)
                        }
                        3 -> {
                            ChipInput(
                                label = "Conditions",
                                value = conditions,
                                onValueChange = { conditions = it },
                                helperText = "Optional. Separate with commas.",
                                chips = ProfileValidation.splitList(conditions),
                            )
                            ChipInput(
                                label = "Medications",
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
                        4 -> {
                            HealthTextField("Surgery", surgery, { surgery = it }, helperText = "Optional, like appendectomy or knee repair.")
                            HealthDateField(
                                label = "Discharge date",
                                digits = dischargeDigits,
                                onDigitsChange = { dischargeDigits = it },
                                helperText = "Optional. Use MM/DD/YYYY.",
                                errorText = dischargeDateError,
                            )
                            HealthTextField("Address", address, { address = it }, helperText = "Optional. Used for caregiver and care coordination context.")
                        }
                        else -> {
                            HealthTextField("Emergency contact name", emergencyName, { emergencyName = it }, errorText = emergencyNameError)
                            HealthTextField("Emergency contact phone", emergencyPhone, { emergencyPhone = it }, errorText = emergencyPhoneError, keyboardType = KeyboardType.Phone)
                            HealthTextField("Caregiver name", caregiverName, { caregiverName = it }, helperText = "Optional.")
                            HealthTextField("Caregiver contact", caregiverContact, { caregiverContact = it }, helperText = "Optional phone number.", errorText = caregiverContactError, keyboardType = KeyboardType.Phone)
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
                                patientId = PatientIdGenerator.newUuid(),
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                dob = dob.trim(),
                                sex = sex,
                                address = address.trim().ifBlank { null },
                                heightFeet = feet.toInt(),
                                heightInches = inches.toInt(),
                                weightPounds = weight.toInt(),
                                surgery = surgery.trim().ifBlank { null },
                                dischargeDate = dischargeDate.trim().ifBlank { null },
                                emergencyContactName = emergencyName.trim(),
                                emergencyContactPhone = emergencyPhone.trim(),
                                caregiverName = caregiverName.trim().ifBlank { null },
                                caregiverContact = caregiverContact.trim().ifBlank { null },
                            ),
                            MedicalProfile(
                                conditions = ProfileValidation.splitList(conditions),
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
