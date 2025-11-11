package com.service.api.model

import com.service.api.common.InterestField
import com.service.api.common.enum.AgeGroup
import com.service.api.common.enum.District
import com.service.api.common.enum.InterestLevel
import com.service.api.common.enum.VoterType
import java.time.LocalDate

data class UserVoteEligibility(
    val district: District?,
    val interestFields: Set<InterestField>?,
    val interestLevel: InterestLevel?,
    val isForeigner: Boolean?,
    val birthdate: LocalDate?,
) {
    companion object {
        fun makeVoterType(userVoteEligibility: UserVoteEligibility): VoterType {
            with (userVoteEligibility) {
                val isProfileCompleted = district != null && interestFields != null && interestLevel != null

                if (!isProfileCompleted || isForeigner == null || birthdate == null)
                    return VoterType.INCOMPLETE

                if (isForeigner)
                    return VoterType.FOREIGNER

                val age = AgeGroup.getAge(birthdate)
                if (age < 18)
                    return VoterType.UNDERAGE

                return VoterType.ELIGIBLE
            }
        }
    }
}
