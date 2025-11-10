package com.service.api.dto

import com.service.api.model.Device
import com.service.api.model.TermsAgreement

data class SignupRequest(
    val social: Social,
    val termsAgreements: List<TermsAgreement>,
    val device: Device,
)
