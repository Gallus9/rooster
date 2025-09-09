package com.example.rooster

import com.parse.ParseFile
import com.parse.ParseObject
import java.util.Date

// Milestone tracking data models for enhanced traceability

data class MilestoneType(
    val id: String,
    val name: String,
    val description: String,
    val requiresPhoto: Boolean,
    val ageWeeksMin: Int,
    val ageWeeksMax: Int,
    val requiredFields: List<String>,
)

// Pre-defined milestone types
object MilestoneTypes {
    val BIRTH =
        MilestoneType(
            id = "birth",
            name = "Birth Record",
            description = "Initial birth documentation with parent information",
            requiresPhoto = true,
            ageWeeksMin = 0,
            ageWeeksMax = 0,
            requiredFields = listOf("parentId", "birthWeight", "photo"),
        )

    val VACCINATION_WEEK_1 =
        MilestoneType(
            id = "vaccination_week1",
            name = "First Vaccination",
            description = "Initial vaccination at 1 week",
            requiresPhoto = true,
            ageWeeksMin = 1,
            ageWeeksMax = 1,
            requiredFields = listOf("vaccineType", "vaccineBatch", "photo"),
        )

    val VACCINATION_WEEK_3 =
        MilestoneType(
            id = "vaccination_week3",
            name = "Second Vaccination",
            description = "Follow-up vaccination at 3 weeks",
            requiresPhoto = true,
            ageWeeksMin = 3,
            ageWeeksMax = 3,
            requiredFields = listOf("vaccineType", "vaccineBatch", "photo"),
        )

    val FIVE_WEEK_ASSESSMENT =
        MilestoneType(
            id = "five_week_assessment",
            name = "5-Week Health Check",
            description = "Comprehensive health assessment at 5 weeks",
            requiresPhoto = true,
            ageWeeksMin = 5,
            ageWeeksMax = 5,
            requiredFields = listOf("weight", "height", "healthStatus", "photo"),
        )

    val INTERVAL_UPDATE =
        MilestoneType(
            id = "interval_update",
            name = "5-Week Interval Update",
            description = "Regular 5-week interval health and growth update",
            requiresPhoto = true,
            ageWeeksMin = 10,
            ageWeeksMax = 20,
            requiredFields = listOf("weight", "healthStatus", "photo"),
        )

    val TWENTY_WEEK_ASSESSMENT =
        MilestoneType(
            id = "twenty_week_assessment",
            name = "20-Week Major Assessment",
            description = "Critical assessment for color, gender, physical development",
            requiresPhoto = true,
            ageWeeksMin = 20,
            ageWeeksMax = 20,
            requiredFields =
                listOf(
                    "color",
                    "confirmedGender",
                    "weight",
                    "height",
                    "photo",
                    "physicalCondition",
                ),
        )

    val WEEKLY_UPDATE =
        MilestoneType(
            id = "weekly_update",
            name = "Weekly Progress Update",
            description = "Optional weekly updates from 20-52 weeks",
            requiresPhoto = false,
            ageWeeksMin = 21,
            ageWeeksMax = 52,
            requiredFields = listOf("weight", "healthStatus"),
        )

    val BREEDER_QUALIFICATION =
        MilestoneType(
            id = "breeder_qualification",
            name = "Breeder Qualification",
            description = "Final assessment for breeder status qualification",
            requiresPhoto = true,
            ageWeeksMin = 35,
            ageWeeksMax = 52,
            requiredFields =
                listOf(
                    "color",
                    "weight",
                    "height",
                    "reproductiveHealth",
                    "photo",
                    "qualificationScore",
                ),
        )

    val MORTALITY_RECORD =
        MilestoneType(
            id = "mortality_record",
            name = "Mortality Record",
            description = "Documentation of fowl mortality with cause",
            requiresPhoto = true,
            ageWeeksMin = 0,
            ageWeeksMax = 999,
            requiredFields = listOf("causeOfDeath", "photo", "veterinaryReport"),
        )
}

data class MilestoneData(
    val objectId: String,
    val fowlId: String,
    val milestoneType: String,
    val recordedDate: Date,
    val fowlAgeWeeks: Int,
    val recordedBy: String,
    val photo: ParseFile?,
    val photoVerificationHash: String?,
    val gpsLocation: String?,
    val weight: Double?,
    val height: Double?,
    val color: String?,
    val gender: String?,
    val healthStatus: String?,
    val vaccineType: String?,
    val vaccineBatch: String?,
    val physicalCondition: String?,
    val reproductiveHealth: String?,
    val qualificationScore: Int?,
    val causeOfDeath: String?,
    val parentId: String?,
    val birthWeight: Double?,
    val veterinaryReport: String?,
    val notes: String?,
    val isVerified: Boolean,
    val verificationTimestamp: Date?,
    val fraudPreventionData: Map<String, Any>?,
)

data class FowlMilestoneProgress(
    val fowlId: String,
    val completedMilestones: List<MilestoneData>,
    val pendingMilestones: List<MilestoneType>,
    val overdueMilestones: List<MilestoneType>,
    val currentAgeWeeks: Int,
    val isBreederQualified: Boolean,
    val mortalityStatus: MilestoneData?,
)

// Parse backend utility functions
object MilestoneParser {
    fun createMilestoneRecord(milestone: MilestoneData): ParseObject {
        val parseObject = ParseObject("FowlMilestone")
        parseObject.put("fowlId", milestone.fowlId)
        parseObject.put("milestoneType", milestone.milestoneType)
        parseObject.put("recordedDate", milestone.recordedDate)
        parseObject.put("fowlAgeWeeks", milestone.fowlAgeWeeks)
        parseObject.put("recordedBy", milestone.recordedBy)
        milestone.photo?.let { parseObject.put("photo", it) }
        milestone.photoVerificationHash?.let { parseObject.put("photoVerificationHash", it) }
        milestone.gpsLocation?.let { parseObject.put("gpsLocation", it) }
        milestone.weight?.let { parseObject.put("weight", it) }
        milestone.height?.let { parseObject.put("height", it) }
        milestone.color?.let { parseObject.put("color", it) }
        milestone.gender?.let { parseObject.put("gender", it) }
        milestone.healthStatus?.let { parseObject.put("healthStatus", it) }
        milestone.vaccineType?.let { parseObject.put("vaccineType", it) }
        milestone.vaccineBatch?.let { parseObject.put("vaccineBatch", it) }
        milestone.physicalCondition?.let { parseObject.put("physicalCondition", it) }
        milestone.reproductiveHealth?.let { parseObject.put("reproductiveHealth", it) }
        milestone.qualificationScore?.let { parseObject.put("qualificationScore", it) }
        milestone.causeOfDeath?.let { parseObject.put("causeOfDeath", it) }
        milestone.parentId?.let { parseObject.put("parentId", it) }
        milestone.birthWeight?.let { parseObject.put("birthWeight", it) }
        milestone.veterinaryReport?.let { parseObject.put("veterinaryReport", it) }
        milestone.notes?.let { parseObject.put("notes", it) }
        parseObject.put("isVerified", milestone.isVerified)
        milestone.verificationTimestamp?.let { parseObject.put("verificationTimestamp", it) }
        milestone.fraudPreventionData?.let {
            parseObject.put("fraudPreventionData", it)
        }
        return parseObject
    }

    fun parseObjectToMilestone(parseObject: ParseObject): MilestoneData {
        return MilestoneData(
            objectId = parseObject.objectId,
            fowlId = parseObject.getString("fowlId") ?: "",
            milestoneType = parseObject.getString("milestoneType") ?: "",
            recordedDate = parseObject.getDate("recordedDate") ?: Date(),
            fowlAgeWeeks = parseObject.getInt("fowlAgeWeeks"),
            recordedBy = parseObject.getString("recordedBy") ?: "",
            photo = parseObject.getParseFile("photo"),
            photoVerificationHash = parseObject.getString("photoVerificationHash"),
            gpsLocation = parseObject.getString("gpsLocation"),
            weight = parseObject.getDouble("weight").takeIf { it != 0.0 },
            height = parseObject.getDouble("height").takeIf { it != 0.0 },
            color = parseObject.getString("color"),
            gender = parseObject.getString("gender"),
            healthStatus = parseObject.getString("healthStatus"),
            vaccineType = parseObject.getString("vaccineType"),
            vaccineBatch = parseObject.getString("vaccineBatch"),
            physicalCondition = parseObject.getString("physicalCondition"),
            reproductiveHealth = parseObject.getString("reproductiveHealth"),
            qualificationScore = parseObject.getInt("qualificationScore").takeIf { it != 0 },
            causeOfDeath = parseObject.getString("causeOfDeath"),
            parentId = parseObject.getString("parentId"),
            birthWeight = parseObject.getDouble("birthWeight").takeIf { it != 0.0 },
            veterinaryReport = parseObject.getString("veterinaryReport"),
            notes = parseObject.getString("notes"),
            isVerified = parseObject.getBoolean("isVerified"),
            verificationTimestamp = parseObject.getDate("verificationTimestamp"),
            fraudPreventionData = parseObject.getMap("fraudPreventionData"),
        )
    }
}
