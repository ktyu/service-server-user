package com.service.api.controller

import com.service.api.model.Terms
import com.service.api.service.TermsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TermsController(
    private val termsService: TermsService,
) {

    @GetMapping("/v1/terms")
    fun getAllTerms(): List<Terms> {
        return termsService.getAllTerms()
    }
}
