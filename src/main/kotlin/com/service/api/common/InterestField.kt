package com.service.api.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class InterestField @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    @get:JsonValue
    val value: String,
) {
    companion object {
        private val validFields = setOf(
            "분야이름1",
            "분야이름2",
            "분야이름3",
            "분야이름4",
            "분야이름5",
            "분야이름6",
            "분야이름7",
        )
    }

    init {
        require(value in validFields) { "Invalid InterestField: $value" }
    }
}
