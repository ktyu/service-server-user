package com.service.api.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SaveIdentityRequest(
    @field:JsonProperty("MDL_TKN")
    val MDL_TKN: String,
)
