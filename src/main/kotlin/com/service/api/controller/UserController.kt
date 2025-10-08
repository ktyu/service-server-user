package com.service.api.controller

import com.service.api.common.ApiRequestContextHolder
import com.service.api.interceptor.Auth
import com.service.api.model.User
import com.service.api.service.UserService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
) {

    @GetMapping("/v1/user")
    @Auth
    fun getCurrentUser(@RequestParam(required = false) attributes: List<String>?): User {
        val ctx = ApiRequestContextHolder.get()
        return userService.getUserWithDevice(ctx.serviceUserId!!, ctx.customDeviceId)
        // TODO: attributes 가 null이 아니면 요청한 값만 리턴해야함
    }

    @PutMapping("/v1/user")
    @Auth
    fun modifyCurrentUser() {
        TODO() // TODO: 구현
    }

    @DeleteMapping("/v1/user")
    @Auth
    fun deleteCurrentUser() {
        userService.deleteUser(ApiRequestContextHolder.get().serviceUserId!!)
    }

    @GetMapping("/v1/user/firebase-custom-token")
    @Auth
    fun getCurrentUserFirebaseCustomToken() {
        TODO() // TODO: 구현
    }

    @GetMapping("/v1/user/nickname-availability")
    @Auth
    fun checkNicknameAvailability() {
        TODO() // TODO: 구현
    }
}
