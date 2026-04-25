package org.sova.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
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
import sova.composeapp.generated.resources.Res
import sova.composeapp.generated.resources.sova_mascot
import sova.composeapp.generated.resources.sova_mascot_eyes_closed
import sova.composeapp.generated.resources.sova_mascot_smiling

@Composable
fun OnboardingScreen(
    patientId: String,
    onComplete: (UserProfile, MedicalProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(0) }
    var dobDigits by remember { mutableStateOf("") }
    var dobFutureAttempted by remember { mutableStateOf(false) }
    var sex by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var surgery by remember { mutableStateOf("") }
    var dischargeDigits by remember { mutableStateOf("") }
    var dischargeFutureAttempted by remember { mutableStateOf(false) }
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
    val dobError = if (dobFutureAttempted) {
        "Date of birth cannot be in the future."
    } else {
        visibleError(dobDigits) { ProfileValidation.dobError(dob) }
    }
    val sexError: String? = null
    val dischargeDate = formatDateOfBirth(dischargeDigits)
    val surgeryError = visibleError(surgery) { ProfileValidation.requiredError(it, "Surgery") }
    val dischargeDateError = if (dischargeFutureAttempted) {
        "Discharge date cannot be in the future."
    } else {
        visibleError(dischargeDigits) { ProfileValidation.dateError(dischargeDate, "Discharge date") }
    }
    val emergencyNameError = visibleError(emergencyName) { ProfileValidation.requiredError(it, "Emergency contact name") }
    val emergencyPhoneError = visibleError(emergencyPhone) { ProfileValidation.phoneError(it, required = true) }
    val doctorPhoneError = visibleError(doctorPhoneNumber) { ProfileValidation.phoneError(it, required = false) }

    val currentStepValid = when (step) {
        0 -> !dobFutureAttempted && ProfileValidation.dobError(dob) == null && sex.isNotBlank()
        1 -> ProfileValidation.requiredError(surgery, "Surgery") == null &&
            !dischargeFutureAttempted &&
            ProfileValidation.dateError(dischargeDate, "Discharge date") == null
        2 -> true
        else -> ProfileValidation.requiredError(emergencyName, "Emergency contact name") == null &&
            ProfileValidation.phoneError(emergencyPhone, required = true) == null &&
            ProfileValidation.phoneError(doctorPhoneNumber, required = false) == null
    }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
        item {
            ImmersiveOnboardingHero(
                title = steps[step].title,
                subtitle = steps[step].subtitle,
                stepLabel = "Step ${step + 1} of ${steps.size}",
                progress = (step + 1).toFloat() / steps.size.toFloat(),
                step = step,
            )
        }
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                HealthCard(modifier = Modifier.widthIn(max = 440.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                        when (step) {
                            0 -> {
                                HealthDateField(
                                    label = "Date of birth",
                                    digits = dobDigits,
                                    onDigitsChange = { candidate ->
                                        if (candidate.isCompleteFutureDate(::formatDateOfBirth, ProfileValidation::dobError)) {
                                            dobFutureAttempted = true
                                        } else {
                                            dobFutureAttempted = false
                                            dobDigits = candidate
                                        }
                                    },
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
                                    onDigitsChange = { candidate ->
                                        if (candidate.isCompleteFutureDate(::formatDateOfBirth) { value ->
                                                ProfileValidation.dateError(value, "Discharge date")
                                            }
                                        ) {
                                            dischargeFutureAttempted = true
                                        } else {
                                            dischargeFutureAttempted = false
                                            dischargeDigits = candidate
                                        }
                                    },
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
        }
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                PrimaryButton(
                    text = if (step == steps.lastIndex) "Start monitoring" else "Continue",
                    enabled = currentStepValid,
                    modifier = Modifier.widthIn(max = 440.dp),
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
        }
        if (step > 0) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    SecondaryButton("Back", onClick = { step -= 1 }, modifier = Modifier.widthIn(max = 440.dp))
                }
            }
        }
    }
}

private data class WizardStep(
    val title: String,
    val subtitle: String,
)

@Composable
private fun ImmersiveOnboardingHero(
    title: String,
    subtitle: String,
    stepLabel: String,
    progress: Float,
    step: Int,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val large = maxWidth >= HealthSpacing.DesktopBreakpoint
        val mascotSize = if (large) 560.dp else 240.dp
        val portraitSize = if (large) 620.dp else 260.dp
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
        ) {
            Text(title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.headlineLarge)
            Text(subtitle, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            OnboardingMascotStage(step = step, portraitSize = portraitSize, mascotSize = mascotSize)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                color = HealthColors.Accent,
                trackColor = HealthColors.SurfaceSubtle,
                gapSize = HealthSpacing.None,
                drawStopIndicator = {},
            )
            Text(stepLabel, color = HealthColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun OnboardingMascotStage(
    step: Int,
    portraitSize: androidx.compose.ui.unit.Dp = 230.dp,
    mascotSize: androidx.compose.ui.unit.Dp = 210.dp,
) {
    val transition = rememberInfiniteTransition(label = "sova-onboarding")
    val bob by transition.animateFloat(
        initialValue = -3f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(animation = tween(1100), repeatMode = RepeatMode.Reverse),
        label = "sova-bob",
    )
    val peek by transition.animateFloat(
        initialValue = 0f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(animation = tween(1300), repeatMode = RepeatMode.Reverse),
        label = "sova-peek",
    )

    val mood = when (step) {
        0 -> MascotMood(
            image = Res.drawable.sova_mascot_eyes_closed,
            message = "I’ll keep this private.",
            tucked = true,
        )
        1 -> MascotMood(
            image = Res.drawable.sova_mascot,
            message = "Tell me what recovery looks like.",
            tucked = false,
        )
        2 -> MascotMood(
            image = Res.drawable.sova_mascot_eyes_closed,
            message = "Sensitive notes stay calm and tidy.",
            tucked = true,
        )
        else -> MascotMood(
            image = Res.drawable.sova_mascot_smiling,
            message = "Almost ready to watch with you.",
            tucked = false,
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = HealthSpacing.Xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
    ) {
        Box(
            modifier = Modifier
                .size(portraitSize)
                .clip(HealthShapes.Pill)
                .background(HealthColors.AccentSoft),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Crossfade(targetState = mood, label = "sova-mood") { target ->
                Image(
                    painter = painterResource(target.image),
                    contentDescription = "Sova mascot",
                    modifier = Modifier
                        .size(mascotSize)
                        .offset(y = if (target.tucked) (42 - peek * 1.8f).dp else bob.dp)
                        .graphicsLayer {
                            rotationZ = if (target.tucked) -3f else bob * 0.28f
                        },
                )
            }
        }
        Text(mood.message, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
    }
}

private data class MascotMood(
    val image: DrawableResource,
    val message: String,
    val tucked: Boolean,
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

private fun String.isCompleteFutureDate(format: (String) -> String, validator: (String) -> String?): Boolean =
    length == 8 && validator(format(this))?.contains("future", ignoreCase = true) == true
