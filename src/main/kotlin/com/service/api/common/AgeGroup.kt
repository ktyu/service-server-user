package com.service.api.common

import java.time.LocalDate
import java.time.Period

data class AgeGroup(val value: String) {
    companion object {
        private val validFields = setOf(
            "18세 미만",
            "10대",
            "20대",
            "30대",
            "40대",
            "50대",
            "60대",
            "70대",
            "80대 이상",
        )

        fun getAge(birthdate: LocalDate): Int {
            return Period.between(birthdate, LocalDate.now()).years
        }

        fun fromBirthdate(birthdate: LocalDate): AgeGroup {
            val age = getAge(birthdate)
            return AgeGroup(when {
                age < 18 -> "18세 미만"
                age < 20 -> "10대"
                age < 30 -> "20대"
                age < 40 -> "30대"
                age < 50 -> "40대"
                age < 60 -> "50대"
                age < 70 -> "60대"
                age < 80 -> "70대"
                age >= 80 -> "80대 이상"
                else -> throw RuntimeException("age is invalid: $age")
            })
        }
    }

    init {
        require(value in validFields) { "Invalid AgeGroup: $value" }
    }
}
