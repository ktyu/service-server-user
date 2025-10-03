package com.service.api.controller

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UserController(

) {

    @GetMapping("/v1/user")
    fun getCurrentUser() {

    }

    @PutMapping("/v1/user")
    fun modifyCurrentUser() {

    }

    @DeleteMapping("/v1/user")
    fun deleteCurrentUser() {

    }

    @GetMapping("/v1/user/firebase-custom-token")
    fun getCurrentUserFirebaseCustomToken() {

    }

    @GetMapping("/v1/user/nickname-availability")
    fun checkNicknameAvailability() {

    }
}
