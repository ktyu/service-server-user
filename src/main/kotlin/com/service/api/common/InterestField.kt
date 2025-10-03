package com.service.api.common

data class InterestField(val value: String) {
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
