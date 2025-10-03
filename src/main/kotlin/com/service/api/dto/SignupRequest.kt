package com.service.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.service.api.model.TermsAgreement

data class SignupRequest(
    val social: Social,
    val termsAgreements: List<TermsAgreement>,
    val identity: Identity,
    val device: Device,
)

data class Identity(
    @field:JsonProperty("MDL_TKN")
    val MDL_TKN: String,
)
