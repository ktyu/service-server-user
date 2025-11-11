package com.service.api.controller

import com.service.api.dto.Response
import com.service.api.dto.TermsResponse
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
    fun getAllTerms(): Response<TermsResponse> {
        return Response.success(TermsResponse(termsService.getAllTerms()))
    }
}
