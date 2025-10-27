package com.service.api.controller

import com.service.api.common.ApiRequestContextHolder
import com.service.api.dto.FirebaseCustomTokenResponse
import com.service.api.dto.ModifyUserRequest
import com.service.api.interceptor.Auth
import com.service.api.model.User
import com.service.api.service.UserService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
) {

    @GetMapping("/v1/user")
    @Auth
    fun getCurrentUser(): User {
        val ctx = ApiRequestContextHolder.get()
        return userService.getUserWithDevice(ctx.serviceUserId!!, ctx.customDeviceId)
    }

    @PutMapping("/v1/user")
    @Auth
    fun modifyCurrentUser(@Valid @RequestBody req: ModifyUserRequest) {
        val ctx = ApiRequestContextHolder.get()
        userService.updateUser(ctx.serviceUserId!!, ctx.customDeviceId, req.profile, req.device)
    }

    @DeleteMapping("/v1/user")
    @Auth
    fun deleteCurrentUser() {
        val ctx = ApiRequestContextHolder.get()
        userService.deleteUser(ctx.serviceUserId!!)
    }

    @GetMapping("/v1/user/firebase-custom-token")
    @Auth
    fun getCurrentUserFirebaseCustomToken(): FirebaseCustomTokenResponse {
        val ctx = ApiRequestContextHolder.get()
        return FirebaseCustomTokenResponse(
            userService.makeFirebaseCustomToken(ctx.serviceUserId!!, ctx.customDeviceId, ctx.osType, ctx.deviceModel)
        )
    }

    @GetMapping("/v1/user/nickname-availability")
    @Auth
    fun checkNicknameAvailability(@RequestParam nickname: String): NicknameAvailabilityResponse {
        return nickname.trim().let {
            NicknameAvailabilityResponse(
                nickname = it,
                isForbidden = userService.isAllowedNickname(it).not(),
                isOccupied = userService.isExistingNickname(it),
            )
        }
    }
}
