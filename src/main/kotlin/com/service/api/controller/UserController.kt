package com.service.api.controller

import com.service.api.common.ApiRequestContextHolder
import com.service.api.dto.FirebaseCustomTokenResponse
import com.service.api.dto.ModifyUserRequest
import com.service.api.dto.NicknameAvailabilityResponse
import com.service.api.dto.Response
import com.service.api.interceptor.Auth
import com.service.api.model.User
import com.service.api.service.UserService
import com.service.api.service.social.SocialService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
    private val socialService: SocialService,
) {

    @GetMapping("/v1/user")
    @Auth
    fun getCurrentUser(): Response<User> {
        val ctx = ApiRequestContextHolder.get()
        return Response.success(
            userService.getUserWithDevice(ctx.serviceUserId!!, ctx.customDeviceId)
        )
    }

    @PutMapping("/v1/user")
    @Auth
    fun modifyCurrentUser(@Valid @RequestBody req: ModifyUserRequest): Response<Void> {
        val ctx = ApiRequestContextHolder.get()
        userService.updateUser(ctx.serviceUserId!!, ctx.customDeviceId, req.profile, req.device)

        return Response(Response.SUCCESS)
    }

    @DeleteMapping("/v1/user")
    @Auth
    fun deleteCurrentUser(): Response<Void> {
        val ctx = ApiRequestContextHolder.get()
        userService.deleteUser(ctx.serviceUserId!!)

        return Response(Response.SUCCESS)
    }

    @GetMapping("/v1/user/firebase-custom-token")
    @Auth
    fun getCurrentUserFirebaseCustomToken(): Response<FirebaseCustomTokenResponse> {
        val ctx = ApiRequestContextHolder.get()
        return Response.success(FirebaseCustomTokenResponse(
            userService.makeFirebaseCustomToken(ctx.serviceUserId!!, ctx.customDeviceId, ctx.osType, ctx.deviceModel)
        ))
    }

    @DeleteMapping("/v1/user/social-account/{id}")
    @Auth
    fun deleteSocialAccount(@PathVariable id: Long): Response<Void> {
        val ctx = ApiRequestContextHolder.get()
        socialService.deleteAndRevokeSocialStatus(serviceUserId = ctx.serviceUserId, socialId = id)

        return Response(Response.SUCCESS)
    }

    @GetMapping("/v1/user/nickname-availability")
    @Auth
    fun checkNicknameAvailability(@RequestParam nickname: String): Response<NicknameAvailabilityResponse> {
        return Response.success(nickname.trim().let {
            NicknameAvailabilityResponse(
                nickname = it,
                isForbidden = userService.isAllowedNickname(it).not(),
                isOccupied = userService.isExistingNickname(it),
            )
        })
    }
}
