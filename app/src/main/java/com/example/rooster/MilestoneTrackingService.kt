package com.example.rooster

import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MilestoneTrackingService {
    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    fun calculateAgeInWeeks(birthDate: String): Int {
        return try {
            val birth = dateFormat.parse(birthDate)
            val now = Date()
            val diffInMillis = now.time - (birth?.time ?: 0)
            val weeks = TimeUnit.MILLISECONDS.toDays(diffInMillis) / 7
            weeks.toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun getAvailableMilestones(
        fowlType: String,
        currentAgeWeeks: Int,
    ): List<MilestoneType> {
        val milestones = mutableListOf<MilestoneType>()

        milestones.add(MilestoneTypes.BIRTH)
        milestones.add(MilestoneTypes.VACCINATION_WEEK_1)
        milestones.add(MilestoneTypes.VACCINATION_WEEK_3)
        milestones.add(MilestoneTypes.FIVE_WEEK_ASSESSMENT)
        milestones.add(MilestoneTypes.MORTALITY_RECORD)

        if (currentAgeWeeks >= 10) {
            milestones.add(MilestoneTypes.INTERVAL_UPDATE)
        }

        if (currentAgeWeeks >= 20) {
            milestones.add(MilestoneTypes.TWENTY_WEEK_ASSESSMENT)
            milestones.add(MilestoneTypes.WEEKLY_UPDATE)
        }

        val breederQualificationAge = if (fowlType.equals("Hen", ignoreCase = true)) 35 else 52
        if (currentAgeWeeks >= breederQualificationAge) {
            milestones.add(MilestoneTypes.BREEDER_QUALIFICATION)
        }

        return milestones
    }

    fun getPendingMilestones(
        fowlType: String,
        currentAgeWeeks: Int,
        completedMilestones: List<MilestoneData>,
    ): List<MilestoneType> {
        val availableMilestones = getAvailableMilestones(fowlType, currentAgeWeeks)
        val completedTypes = completedMilestones.map { it.milestoneType }.toSet()

        return availableMilestones.filter { milestone ->
            !completedTypes.contains(milestone.id) &&
                currentAgeWeeks >= milestone.ageWeeksMin &&
                (milestone.ageWeeksMax == 999 || currentAgeWeeks <= milestone.ageWeeksMax + 2)
        }
    }

    fun getOverdueMilestones(
        fowlType: String,
        currentAgeWeeks: Int,
        completedMilestones: List<MilestoneData>,
    ): List<MilestoneType> {
        val availableMilestones = getAvailableMilestones(fowlType, currentAgeWeeks)
        val completedTypes = completedMilestones.map { it.milestoneType }.toSet()

        return availableMilestones.filter { milestone ->
            !completedTypes.contains(milestone.id) &&
                currentAgeWeeks > milestone.ageWeeksMax + 2 &&
                milestone.ageWeeksMax != 999
        }
    }

    fun checkBreederQualification(
        fowlType: String,
        completedMilestones: List<MilestoneData>,
    ): Boolean {
        val requiredAge = if (fowlType.equals("Hen", ignoreCase = true)) 35 else 52

        val breederQualification =
            completedMilestones.find {
                it.milestoneType == MilestoneTypes.BREEDER_QUALIFICATION.id
            }

        if (breederQualification == null) return false

        val twentyWeekAssessment =
            completedMilestones.find {
                it.milestoneType == MilestoneTypes.TWENTY_WEEK_ASSESSMENT.id
            }

        if (twentyWeekAssessment == null) return false

        val qualificationScore = breederQualification.qualificationScore ?: 0

        return qualificationScore >= 70 && breederQualification.fowlAgeWeeks >= requiredAge
    }

    fun generateFraudPreventionData(): Map<String, Any> {
        return mapOf(
            "timestamp" to System.currentTimeMillis(),
            "deviceId" to "android_device",
            "appVersion" to "1.0.0",
            "recordingSession" to java.util.UUID.randomUUID().toString(),
            "networkType" to "mobile",
        )
    }

    fun validateMilestoneData(
        milestoneType: MilestoneType,
        milestoneData: MilestoneData,
    ): ValidationResult {
        val errors = mutableListOf<String>()

        milestoneType.requiredFields.forEach { field ->
            when (field) {
                "photo" -> if (milestoneData.photo == null) errors.add("Photo is required")
                "weight" ->
                    if (milestoneData.weight == null || milestoneData.weight <= 0) {
                        errors.add("Valid weight is required")
                    }

                "height" ->
                    if (milestoneData.height == null || milestoneData.height <= 0) {
                        errors.add("Valid height is required")
                    }

                "color" ->
                    if (milestoneData.color.isNullOrBlank()) {
                        errors.add("Color description is required")
                    }

                "healthStatus" ->
                    if (milestoneData.healthStatus.isNullOrBlank()) {
                        errors.add("Health status is required")
                    }

                "vaccineType" ->
                    if (milestoneData.vaccineType.isNullOrBlank()) {
                        errors.add("Vaccine type is required")
                    }

                "vaccineBatch" ->
                    if (milestoneData.vaccineBatch.isNullOrBlank()) {
                        errors.add("Vaccine batch number is required")
                    }

                "causeOfDeath" ->
                    if (milestoneData.causeOfDeath.isNullOrBlank()) {
                        errors.add("Cause of death is required")
                    }

                "parentId" ->
                    if (milestoneData.parentId.isNullOrBlank()) {
                        errors.add("Parent fowl selection is required")
                    }
            }
        }

        if (milestoneData.fowlAgeWeeks < milestoneType.ageWeeksMin) {
            errors.add("Fowl is too young for this milestone (min ${milestoneType.ageWeeksMin} weeks)")
        }

        if (milestoneType.ageWeeksMax != 999 &&
            milestoneData.fowlAgeWeeks > milestoneType.ageWeeksMax + 2
        ) {
            errors.add("Fowl is too old for this milestone (max ${milestoneType.ageWeeksMax} weeks)")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    suspend fun fetchFowlMilestones(
        fowlId: String,
        onResult: (List<MilestoneData>) -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val query = ParseQuery.getQuery<ParseObject>("FowlMilestone")
            query.whereEqualTo("fowlId", fowlId)
            query.orderByDescending("recordedDate")

            query.findInBackground { objects, e ->
                if (e != null) {
                    onError(e.localizedMessage ?: "Failed to fetch milestones")
                } else {
                    val milestones =
                        objects?.map {
                            MilestoneParser.parseObjectToMilestone(it)
                        } ?: emptyList()
                    onResult(milestones)
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to fetch milestones")
        }
    }

    suspend fun saveMilestone(
        milestoneData: MilestoneData,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val parseObject = MilestoneParser.createMilestoneRecord(milestoneData)

            val acl = com.parse.ParseACL(ParseUser.getCurrentUser())
            acl.setPublicReadAccess(true)
            acl.setWriteAccess(ParseUser.getCurrentUser(), true)
            parseObject.acl = acl

            parseObject.saveInBackground { e ->
                if (e != null) {
                    onError(e.localizedMessage ?: "Failed to save milestone")
                } else {
                    parseObject.pinInBackground()
                    onSuccess()
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to save milestone")
        }
    }

    suspend fun getFowlMilestoneProgress(
        fowlId: String,
        fowlType: String,
        birthDate: String,
        onResult: (FowlMilestoneProgress) -> Unit,
        onError: (String) -> Unit,
    ) {
        fetchFowlMilestones(
            fowlId = fowlId,
            onResult = { milestones ->
                val currentAge = calculateAgeInWeeks(birthDate)
                val pending = getPendingMilestones(fowlType, currentAge, milestones)
                val overdue = getOverdueMilestones(fowlType, currentAge, milestones)
                val isBreederQualified = checkBreederQualification(fowlType, milestones)
                val mortalityRecord =
                    milestones.find {
                        it.milestoneType == MilestoneTypes.MORTALITY_RECORD.id
                    }

                val progress =
                    FowlMilestoneProgress(
                        fowlId = fowlId,
                        completedMilestones = milestones,
                        pendingMilestones = pending,
                        overdueMilestones = overdue,
                        currentAgeWeeks = currentAge,
                        isBreederQualified = isBreederQualified,
                        mortalityStatus = mortalityRecord,
                    )

                onResult(progress)
            },
            onError = onError,
        )
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
)
