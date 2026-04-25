package org.sova.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontStyle
import org.sova.components.ChipInput
import org.sova.components.HealthDateField
import org.sova.components.HealthNumberField
import org.sova.components.HealthSegmentedSelector
import org.sova.components.HealthTextField
import org.sova.components.InfoRow
import org.sova.components.JournalCard
import org.sova.components.JournalLabel
import org.sova.components.PrimaryButton
import org.sova.components.SecondaryButton
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.logic.ProfileValidation
import org.sova.model.MedicalProfile
import org.sova.model.UserProfile

@Composable
fun ProfileScreen(
    user: UserProfile,
    medical: MedicalProfile,
    onSave: (UserProfile, MedicalProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingSection by remember { mutableStateOf<ProfileEditSection?>(null) }

    ProfileSummaryScreen(
        user = user,
        medical = medical,
        editingSection = editingSection,
        onEdit = { editingSection = it },
        onCancel = { editingSection = null },
        onSave = { updatedUser, updatedMedical ->
            onSave(updatedUser, updatedMedical)
            editingSection = null
        },
        modifier = modifier,
    )
}

private enum class ProfileEditSection {
    Basic,
    Body,
    Medical,
    Emergency,
    Caregiver,
}

@Composable
private fun ProfileSummaryScreen(
    user: UserProfile,
    medical: MedicalProfile,
    editingSection: ProfileEditSection?,
    onEdit: (ProfileEditSection) -> Unit,
    onCancel: () -> Unit,
    onSave: (UserProfile, MedicalProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Md)) {
                Text(user.fullName, color = HealthColors.TextPrimary, style = MaterialTheme.typography.headlineLarge.copy(fontStyle = FontStyle.Italic))
            }
        }
        item {
            JournalCard {
                if (editingSection == ProfileEditSection.Basic) {
                    BasicInformationEditor(user, medical, onCancel, onSave)
                } else {
                    EditableSectionHeader("Basic information") { onEdit(ProfileEditSection.Basic) }
                    InfoRow("Patient ID", user.patientId)
                    InfoRow("Date of birth", user.dob)
                    InfoRow("Sex", user.sex)
                    InfoRow("Address", user.address?.takeIf { it.isNotBlank() } ?: "Not provided")
                }
            }
        }
        item {
            JournalCard {
                if (editingSection == ProfileEditSection.Body) {
                    BodyMeasurementsEditor(user, medical, onCancel, onSave)
                } else {
                    EditableSectionHeader("Body measurements") { onEdit(ProfileEditSection.Body) }
                    InfoRow("Height", user.heightLabel)
                    InfoRow("Weight", user.weightLabel)
                }
            }
        }
        item {
            JournalCard {
                if (editingSection == ProfileEditSection.Medical) {
                    MedicalInformationEditor(user, medical, onCancel, onSave)
                } else {
                    EditableSectionHeader("Medical information") { onEdit(ProfileEditSection.Medical) }
                    InfoRow("Conditions", readableList(medical.conditions))
                    InfoRow("Medications", readableList(medical.medications))
                    InfoRow("Allergies", readableList(medical.allergies))
                    InfoRow("Surgery", user.surgery?.takeIf { it.isNotBlank() } ?: "Not provided")
                    InfoRow("Discharge date", user.dischargeDate?.takeIf { it.isNotBlank() } ?: "Not provided")
                }
            }
        }
        item {
            JournalCard {
                if (editingSection == ProfileEditSection.Emergency) {
                    EmergencyContactEditor(user, medical, onCancel, onSave)
                } else {
                    EditableSectionHeader("Emergency contact") { onEdit(ProfileEditSection.Emergency) }
                    InfoRow("Name", user.emergencyContactName)
                    InfoRow("Phone", user.emergencyContactPhone)
                }
            }
        }
        item {
            JournalCard {
                if (editingSection == ProfileEditSection.Caregiver) {
                    CaregiverInformationEditor(user, medical, onCancel, onSave)
                } else {
                    EditableSectionHeader("Caregiver information") { onEdit(ProfileEditSection.Caregiver) }
                    InfoRow("Caregiver", user.caregiverName?.takeIf { it.isNotBlank() } ?: "Not provided")
                    InfoRow("Contact", user.caregiverContact?.takeIf { it.isNotBlank() } ?: "Not provided")
                }
            }
        }
        item {
            JournalCard {
                ScenicBlock()
                JournalLabel("Data & privacy")
                Text("Your profile stays on this device unless you choose to share it.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun EditableSectionHeader(title: String, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JournalLabel(title, modifier = Modifier.weight(1f))
        EditPencilButton(onClick = onEdit)
    }
}

@Composable
private fun EditPencilButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(HealthSpacing.Lg)
            .clip(HealthShapes.Pill)
            .background(HealthColors.SurfaceSubtle, HealthShapes.Pill)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(HealthSpacing.Icon)) {
            val stroke = Stroke(width = HealthSpacing.Stroke.toPx() * 1.8f, cap = StrokeCap.Round)
            drawLine(
                color = HealthColors.TextSecondary,
                start = Offset(size.width * 0.28f, size.height * 0.72f),
                end = Offset(size.width * 0.72f, size.height * 0.28f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = HealthColors.TextSecondary,
                start = Offset(size.width * 0.22f, size.height * 0.78f),
                end = Offset(size.width * 0.34f, size.height * 0.74f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun BasicInformationEditor(
    user: UserProfile,
    medical: MedicalProfile,
    onCancel: () -> Unit,
    onSave: (UserProfile, MedicalProfile) -> Unit,
) {
    var firstName by remember(user) { mutableStateOf(user.firstName) }
    var lastName by remember(user) { mutableStateOf(user.lastName) }
    var dobDigits by remember(user) { mutableStateOf(user.dob.filter { it.isDigit() }.take(8)) }
    var sex by remember(user) { mutableStateOf(user.sex) }
    var address by remember(user) { mutableStateOf(user.address.orEmpty()) }
    val dob = formatDateOfBirth(dobDigits)
    val firstNameError = ProfileValidation.nameError(firstName, "First name")
    val lastNameError = ProfileValidation.nameError(lastName, "Last name")
    val dobError = ProfileValidation.dobError(dob)
    val sexError = ProfileValidation.requiredError(sex, "Sex")
    val canSave = listOf(firstNameError, lastNameError, dobError, sexError).all { it == null }

    JournalLabel("Basic information")
    HealthTextField("First name", firstName, { firstName = it }, errorText = firstNameError)
    HealthTextField("Last name", lastName, { lastName = it }, errorText = lastNameError)
    HealthDateField("Date of birth", dobDigits, { dobDigits = it }, helperText = "MM/DD/YYYY", errorText = dobError)
    HealthSegmentedSelector("Sex", listOf("Female", "Male", "Other"), sex, { sex = it }, errorText = sexError)
    HealthTextField("Address", address, { address = it }, helperText = "Optional")
    SectionEditActions(
        canSave = canSave,
        onCancel = onCancel,
        onSave = {
            onSave(
                user.copy(
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    dob = dob,
                    sex = sex,
                    address = address.trim().takeIf { it.isNotBlank() },
                ),
                medical,
            )
        },
    )
}

@Composable
private fun BodyMeasurementsEditor(
    user: UserProfile,
    medical: MedicalProfile,
    onCancel: () -> Unit,
    onSave: (UserProfile, MedicalProfile) -> Unit,
) {
    var feet by remember(user) { mutableStateOf(user.heightFeet.toString()) }
    var inches by remember(user) { mutableStateOf(user.heightInches.toString()) }
    var weight by remember(user) { mutableStateOf(user.weightPounds.toString()) }
    val feetError = ProfileValidation.numberRangeError(feet, "Feet", 1, 8)
    val inchesError = ProfileValidation.numberRangeError(inches, "Inches", 0, 11)
    val weightError = ProfileValidation.numberRangeError(weight, "Weight", 20, 700)
    val canSave = listOf(feetError, inchesError, weightError).all { it == null }

    JournalLabel("Body measurements")
    Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
        HealthNumberField("Feet", feet, { feet = it }, modifier = Modifier.weight(1f), errorText = feetError)
        HealthNumberField("Inches", inches, { inches = it }, modifier = Modifier.weight(1f), errorText = inchesError)
    }
    HealthNumberField("Weight", weight, { weight = it }, helperText = "Pounds", errorText = weightError)
    SectionEditActions(
        canSave = canSave,
        onCancel = onCancel,
        onSave = {
            onSave(
                user.copy(
                    heightFeet = feet.toInt(),
                    heightInches = inches.toInt(),
                    weightPounds = weight.toInt(),
                ),
                medical,
            )
        },
    )
}

@Composable
private fun MedicalInformationEditor(
    user: UserProfile,
    medical: MedicalProfile,
    onCancel: () -> Unit,
    onSave: (UserProfile, MedicalProfile) -> Unit,
) {
    var conditions by remember(medical) { mutableStateOf(medical.conditions.joinToString(", ")) }
    var medications by remember(medical) { mutableStateOf(medical.medications.joinToString(", ")) }
    var allergies by remember(medical) { mutableStateOf(medical.allergies.joinToString(", ")) }
    var surgery by remember(user) { mutableStateOf(user.surgery.orEmpty()) }
    var dischargeDigits by remember(user) { mutableStateOf(user.dischargeDate.orEmpty().filter { it.isDigit() }.take(8)) }
    val dischargeDate = formatDateOfBirth(dischargeDigits)
    val dischargeDateError = ProfileValidation.optionalDateError(dischargeDate, "discharge")

    JournalLabel("Medical information")
    ChipInput("Conditions", conditions, { conditions = it }, helperText = "Separate items with commas.", chips = ProfileValidation.splitList(conditions))
    ChipInput("Medications", medications, { medications = it }, helperText = "Separate items with commas.", chips = ProfileValidation.splitList(medications))
    ChipInput("Allergies", allergies, { allergies = it }, helperText = "Separate items with commas.", chips = ProfileValidation.splitList(allergies))
    HealthTextField("Surgery", surgery, { surgery = it }, helperText = "Optional")
    HealthDateField("Discharge date", dischargeDigits, { dischargeDigits = it }, helperText = "Optional. MM/DD/YYYY", errorText = dischargeDateError)
    SectionEditActions(
        canSave = dischargeDateError == null,
        onCancel = onCancel,
        onSave = {
            onSave(
                user.copy(
                    surgery = surgery.trim().takeIf { it.isNotBlank() },
                    dischargeDate = dischargeDate.trim().takeIf { it.isNotBlank() },
                ),
                medical.copy(
                    conditions = ProfileValidation.splitList(conditions),
                    medications = ProfileValidation.splitList(medications),
                    allergies = ProfileValidation.splitList(allergies),
                ),
            )
        },
    )
}

@Composable
private fun EmergencyContactEditor(
    user: UserProfile,
    medical: MedicalProfile,
    onCancel: () -> Unit,
    onSave: (UserProfile, MedicalProfile) -> Unit,
) {
    var emergencyName by remember(user) { mutableStateOf(user.emergencyContactName) }
    var emergencyPhone by remember(user) { mutableStateOf(user.emergencyContactPhone) }
    val emergencyNameError = ProfileValidation.requiredError(emergencyName, "Emergency contact")
    val emergencyPhoneError = ProfileValidation.phoneError(emergencyPhone, required = true)
    val canSave = listOf(emergencyNameError, emergencyPhoneError).all { it == null }

    JournalLabel("Emergency contact")
    HealthTextField("Emergency contact name", emergencyName, { emergencyName = it }, errorText = emergencyNameError)
    HealthTextField("Emergency contact phone", emergencyPhone, { emergencyPhone = it }, errorText = emergencyPhoneError)
    SectionEditActions(
        canSave = canSave,
        onCancel = onCancel,
        onSave = {
            onSave(
                user.copy(
                    emergencyContactName = emergencyName.trim(),
                    emergencyContactPhone = emergencyPhone.trim(),
                ),
                medical,
            )
        },
    )
}

@Composable
private fun CaregiverInformationEditor(
    user: UserProfile,
    medical: MedicalProfile,
    onCancel: () -> Unit,
    onSave: (UserProfile, MedicalProfile) -> Unit,
) {
    var caregiverName by remember(user) { mutableStateOf(user.caregiverName.orEmpty()) }
    var caregiverContact by remember(user) { mutableStateOf(user.caregiverContact.orEmpty()) }
    val caregiverContactError = ProfileValidation.phoneError(caregiverContact, required = false)
    val canSave = caregiverContactError == null

    JournalLabel("Caregiver information")
    HealthTextField("Caregiver name", caregiverName, { caregiverName = it }, helperText = "Optional")
    HealthTextField("Caregiver contact", caregiverContact, { caregiverContact = it }, helperText = "Optional", errorText = caregiverContactError)
    SectionEditActions(
        canSave = canSave,
        onCancel = onCancel,
        onSave = {
            onSave(
                user.copy(
                    caregiverName = caregiverName.trim().takeIf { it.isNotBlank() },
                    caregiverContact = caregiverContact.trim().takeIf { it.isNotBlank() },
                ),
                medical,
            )
        },
    )
}

@Composable
private fun SectionEditActions(
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
        SecondaryButton("Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
        PrimaryButton("Save", onClick = onSave, modifier = Modifier.weight(1f), enabled = canSave)
    }
}

@Composable
private fun ScenicBlock() {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(HealthSpacing.JournalImageHeight)
            .background(HealthColors.SurfaceSubtle),
    ) {
        drawRect(HealthColors.Success.copy(alpha = 0.20f), size = size)
        drawRect(HealthColors.Warning.copy(alpha = 0.45f), topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.48f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.52f, size.height))
        drawRect(HealthColors.Ink.copy(alpha = 0.75f), topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.72f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.06f, size.height))
    }
}

private fun readableList(items: List<String>): String =
    items.ifEmpty { listOf("None reported") }.joinToString(", ")

private fun formatDateOfBirth(digits: String): String {
    val cleaned = digits.filter { it.isDigit() }.take(8)
    return when {
        cleaned.length <= 2 -> cleaned
        cleaned.length <= 4 -> "${cleaned.take(2)}/${cleaned.drop(2)}"
        else -> "${cleaned.take(2)}/${cleaned.drop(2).take(2)}/${cleaned.drop(4)}"
    }
}
