package com.service.api.common.enum

import java.time.LocalDate
import java.time.Period

enum class AgeGroup {
    UNAUTHENTICATED,
    UNDER_18,
    TEENS,
    TWENTIES,
    THIRTIES,
    FORTIES,
    FIFTIES,
    SIXTIES,
    SEVENTIES,
    EIGHTIES_AND_ABOVE,
    ;

    companion object {
        fun getAge(birthdate: LocalDate): Int {
            return Period.between(birthdate, LocalDate.now()).years
        }

        fun fromBirthdate(birthdate: LocalDate?): AgeGroup {
            if (birthdate == null)
                return UNAUTHENTICATED

            val age = getAge(birthdate)
            return when {
                age < 18 -> UNDER_18
                age < 20 -> TEENS
                age < 30 -> TWENTIES
                age < 40 -> THIRTIES
                age < 50 -> FORTIES
                age < 60 -> FIFTIES
                age < 70 -> SIXTIES
                age < 80 -> SEVENTIES
                age >= 80 -> EIGHTIES_AND_ABOVE
                else -> throw RuntimeException("age is invalid: $age")
            }
        }
    }
}
