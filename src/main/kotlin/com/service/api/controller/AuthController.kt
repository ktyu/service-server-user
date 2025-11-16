package com.service.api.controller

import com.service.api.common.ApiRequestContextHolder
import com.service.api.common.exception.RefreshTokenFailedException
import com.service.api.dto.*
import com.service.api.interceptor.Auth
import com.service.api.service.DeviceService
import com.service.api.service.IdentityService
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
    private val identityService: IdentityService,
) {

    @PostMapping("/v1/auth/social")
    fun saveValidSocial(@Valid @RequestBody req: SaveValidSocialRequest): Response<SaveValidSocialResponse> {
        return with (req) {
            val (socialUuid, serviceUserId) = socialService.saveSocialStatus(socialType, socialAccessToken, socialRefreshToken)

            Response.success(SaveValidSocialResponse(Social(socialType, socialUuid), serviceUserId))
        }
    }

    @PostMapping("/v1/auth/signup")
    fun signup(@Valid @RequestBody req: SignupRequest): Response<SignupResponse> {
        return with (req) {
            val socialId = socialService.validateSocialUuid(social.socialUuid, social.socialType)
            val serviceUserId = userService.createUser(termsAgreements, socialId)
            val (accessToken, refreshToken) = deviceService.saveDevice(serviceUserId, socialId, device.pushTokenType, device.pushToken)

            Response.success(SignupResponse(serviceUserId, accessToken, refreshToken))
        }

    }

    @PostMapping("/v1/auth/login")
    fun login(@Valid @RequestBody req: LoginRequest): Response<LoginResponse> {
        return with (req) {
            val socialId = socialService.validateSocialUuid(social.socialUuid, social.socialType)
            val (accessToken, refreshToken) = deviceService.saveDevice(serviceUserId, socialId, device.pushTokenType, device.pushToken)

            Response.success(LoginResponse(accessToken, refreshToken))
        }
    }

    @PutMapping("/v1/auth/refresh")
    fun refreshTokens(@Valid @RequestBody req: RefreshRequest): Response<RefreshResponse> {
        return with (req) {
            val (accessToken, refreshToken) = deviceService.refreshTokens(serviceUserId, refreshToken)

            Response.success(RefreshResponse(accessToken, refreshToken))
        }
    }

    @PostMapping("/v1/auth/identity")
    @Auth
    fun saveIdentity(@Valid @RequestBody req: SaveIdentityRequest): Response<SaveIdentityResponse> {
        return try { with (req) {
            val ctx = ApiRequestContextHolder.get()
            val mappedServiceUserId = identityService.saveIdentity(MDL_TKN, ctx.serviceUserId!!, ctx.socialId!!)
            if (ctx.serviceUserId!! != mappedServiceUserId) {
                userService.mergeUser(ctx.serviceUserId!!, ctx.socialId!!, mappedServiceUserId)
                val (accessToken, refreshToken) = deviceService.saveDevice(mappedServiceUserId, ctx.socialId!!)

                Response.success(SaveIdentityResponse(true, mappedServiceUserId, accessToken, refreshToken))
            } else {
                Response.success(SaveIdentityResponse(false))
            }
        }} catch (e: Exception) {
            throw RefreshTokenFailedException("$e")
        }
    }

    @DeleteMapping("/v1/auth/logout")
    @Auth
    fun logout(): Response<Void> {
        val ctx = ApiRequestContextHolder.get()
        deviceService.deleteDevice(ctx.serviceUserId!!, ctx.customDeviceId)

        return Response(Response.SUCCESS)
    }
}
