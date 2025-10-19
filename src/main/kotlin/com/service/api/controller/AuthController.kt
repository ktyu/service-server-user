package com.service.api.controller

import com.service.api.common.ApiRequestContextHolder
import com.service.api.dto.*
import com.service.api.interceptor.Auth
import com.service.api.service.DeviceService
import com.service.api.service.social.SocialService
import com.service.api.service.UserService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class AuthController(
    private val socialService: SocialService,
    private val userService: UserService,
    private val deviceService: DeviceService,
) {

    @PostMapping("/v1/auth/social")
    fun saveValidSocial(@Valid @RequestBody req: SaveValidSocialRequest): SaveValidSocialResponse {
        return with (req) {
            val (socialUuid, serviceUserId) = socialService.saveSocialStatus(socialType, socialAccessToken, socialRefreshToken)

            SaveValidSocialResponse(Social(socialType, socialUuid), serviceUserId)
        }
    }

    @PostMapping("/v1/auth/signup")
    fun signup(@Valid @RequestBody req: SignupRequest): SignupResponse {
        return with (req) {
            val serviceUserId = userService.createUser(termsAgreements, social.socialType, social.socialUuid, identity.MDL_TKN)
            val (accessToken, refreshToken) = deviceService.saveDevice(serviceUserId, social.socialType, null, device.pushTokenType, device.pushToken)

            SignupResponse(serviceUserId, accessToken, refreshToken)
        }

    }

    @PostMapping("/v1/auth/login")
    fun login(@Valid @RequestBody req: LoginRequest): LoginResponse {
        return with (req) {
            val (accessToken, refreshToken) = deviceService.saveDevice(serviceUserId, social.socialType, social.socialUuid, device.pushTokenType, device.pushToken)

            LoginResponse(accessToken, refreshToken)
        }
    }

    @PutMapping("/v1/auth/refresh")
    fun refreshTokens(@Valid @RequestBody req: RefreshRequest): RefreshResponse {
        return with (req) {
            val (accessToken, refreshToken) = deviceService.refreshTokens(serviceUserId, refreshToken)

            RefreshResponse(accessToken, refreshToken)
        }
    }

    @DeleteMapping("/v1/auth/logout")
    @Auth
    fun logout() {
        val ctx = ApiRequestContextHolder.get()
        deviceService.deleteDevice(ctx.serviceUserId!!, ctx.customDeviceId)
    }
}
