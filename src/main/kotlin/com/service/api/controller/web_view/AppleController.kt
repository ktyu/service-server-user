package com.service.api.controller.web_view

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/web-view")
class AppleController { // TODO:

    @GetMapping("/apple")
    fun getApplePage(): ResponseEntity<Void> {
        return ResponseEntity
            .status(HttpStatus.FOUND) // 302
            .header(HttpHeaders.LOCATION, "/testtest")
            .build()
    }

    @PostMapping("/callback/apple")
    fun callbackApple() {

    }

    @GetMapping("/apple/exit")
    fun getAppleExitPage() {

    }
}
