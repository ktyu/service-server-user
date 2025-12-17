package com.service.api.model

import com.service.api.common.InterestField
import com.service.api.common.enum.AgeGroup
import com.service.api.common.enum.Region
import com.service.api.common.enum.InterestLevel
import com.service.api.common.enum.VoterType
import java.time.LocalDate

data class UserVoteEligibility(
    val region: Region?,
    val interestFields: Set<InterestField>?,
    val interestLevel: InterestLevel?,
    val issueNote: String?,
    val isForeigner: Boolean?,
    val birthdate: LocalDate?,
) {
    companion object {
        fun makeVoterType(userVoteEligibility: UserVoteEligibility): VoterType {
            with (userVoteEligibility) {
                val isProfileCompleted = region != null && interestFields != null && interestLevel != null

                if (!isProfileCompleted || isForeigner == null || birthdate == null)
                    return VoterType.INCOMPLETE

                if (isForeigner)
                    return VoterType.FOREIGNER

                val age = AgeGroup.getAge(birthdate)
                if (age < 18)
                    return VoterType.UNDERAGE

                if (!issueNote.isNullOrBlank())
                    return VoterType.BLOCKED

                return VoterType.ELIGIBLE
            }
        }
    }
}
