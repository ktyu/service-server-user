package com.service.api.service

import com.google.firebase.auth.FirebaseAuth
import com.service.api.common.AgeGroup
import com.service.api.common.enum.GenderType
import com.service.api.common.enum.OsType
import com.service.api.common.enum.SocialType
import com.service.api.common.enum.VoterType
import com.service.api.common.exception.ServiceUserNotFoundException
import com.service.api.common.exception.Under14SignupFailException
import com.service.api.model.Device
import com.service.api.model.Profile
import com.service.api.model.TermsAgreement
import com.service.api.model.User
import com.service.api.persistence.entity.JpaUserIdentityEntity
import com.service.api.persistence.entity.JpaUserProfileEntity
import com.service.api.persistence.repository.*
import com.service.api.persistence.mapper.DeviceMapper
import com.service.api.persistence.mapper.ProfileMapper
import com.service.api.service.social.SocialService
import com.service.api.util.Sha256HashingUtil
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.util.*
import kotlin.random.Random

@Service
class UserService(
    private val termsService: TermsService,
    private val deviceService: DeviceService,
    private val socialService: SocialService,
    private val userIdentityRepository: UserIdentityRepository,
    private val userProfileRepository: UserProfileRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getUserWithDevice(serviceUserId: Long, customDeviceId: String): User {
        val projection = userIdentityRepository.findUserProjectionByServiceUserIdAndCustomDeviceIdAndDeletedAtIsNull(serviceUserId, customDeviceId)
            ?: throw ServiceUserNotFoundException(serviceUserId)

        return with (projection) {
            val isProfileCompleted = profile.district != null && profile.interestFields != null && profile.interestLevel != null
            User(
                profile = ProfileMapper.toModel(profile),
                device = device?.let { DeviceMapper.toModel(it) },
                email = identity.getLatestSocialUuid()?.let { socialService.getEmail(it) } ?: "",
                voterType = makeVoterType(isProfileCompleted, identity.isForeigner, identity.birthdate),
                ageGroup = AgeGroup.fromBirthdate(identity.birthdate),
                socialTypes = identity.getExistSocials().map { it.first }
            )
        }
    }

    fun isExistingNickname(nickname: String): Boolean {
        return userProfileRepository.existsByNickname(nickname)
    }

    fun isAllowedNickname(nickname: String): Boolean {
        return nickname !in listOf("관리자", "운영자") // TODO: 정확한 정책 정해지면 구현 필요
    }

    @Transactional
    fun createUser(termsAgreements: List<TermsAgreement>, socialType: SocialType, socialUuid: String, identityToken: String): Long {
        // 약관 동의 상태 검사
        val termsAgreementMap = termsService.validateTermsAgreements(termsAgreements)

        // 소셜 연결 상태 검증
        socialService.validateSocialUuid(socialUuid, socialType)

        // TODO: MDL_TKN(identityToken) 값 보내서 본인인증 결과 받아오고 가공하기
        val hashedCi = Sha256HashingUtil.sha256Hex(identityToken, "service")
        val isForeigner: Boolean
        val gender: GenderType
        val birthdate: LocalDate
        try {
            val num = String(Base64.getUrlDecoder().decode(identityToken)).split("@")[1].split("-")
            if ((num[1].toInt() in 1..8).not()) throw RuntimeException()
            isForeigner = num[1].toInt() >= 5 // 뒷자리가 5~8 면 외국인
            gender = if (num[1].toInt() % 2 == 0) GenderType.F else GenderType.M
            birthdate = LocalDate.parse("${if (num[1].toInt() <= 2) 19 else 20}${num[0]}", java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        } catch (e: Exception) {
            throw IllegalArgumentException("invalid MDL_TKN: $identityToken")
        }

        // 만 14세 미만은 가입시도 하면 안됨
        if (AgeGroup.getAge(birthdate) < 14) {
            throw Under14SignupFailException("age is smaller than 14")
        }

        val userIdentityEntity = userIdentityRepository.findByHashedCi(hashedCi)
            ?.apply {
                // 이미 가입된 CI 일 경우 정보 비교
                if (this.getSocialUuid(socialType) == socialUuid)
                    throw IllegalArgumentException("should call login API")
                if (this.deletedAt != null)
                    throw RuntimeException("should not happened. deleted user still has hashedCi")
                if (this.isForeigner != isForeigner || this.gender != gender || this.birthdate != birthdate) {
                    // CI는 그대로인데 본인인증 결과 정보가 달라졌으면 에러 로깅만 남김 (실제 발생은 안할 것으로 기대)
                    log.error("permanent info changed... {}/{}, {}/{}, {}/{}",
                        this.isForeigner, isForeigner, this.gender, gender, this.birthdate, birthdate)
                }

                // 기존 사용자의 정보 중에서 소셜 연결 정보만 전부 날림 (추후 소셜 계정 멀티 매핑 가능하도록 변경되면 수정 필요)
                this.getExistSocials().forEach { (type, uuid) ->
                    this.setSocialUuid(type, null)
                    socialService.deleteSocialUuid(uuid, type, false)
                }
            }
            ?: JpaUserIdentityEntity(
                // 가입되지 않은 CI 일 경우 새로 user 생성
                hashedCi = hashedCi,
                isForeigner = isForeigner,
                gender = gender,
                birthdate = birthdate,
            )
        userIdentityEntity.setSocialUuid(socialType, socialUuid)
        userIdentityRepository.save(userIdentityEntity)

        val userProfileEntity = userProfileRepository.findByIdOrNull(userIdentityEntity.serviceUserId!!)
            ?.apply {
                if (this.deletedAt != null)
                    throw RuntimeException("should not happened. existing user profile is deleted")

                // 기존 사용자였으면 약관 동의 정보만 이번 동의 정보로 바꿔줌
                this.termsAgreements = termsAgreementMap
            }
            ?: JpaUserProfileEntity(
                // 신규 사용자
                serviceUserId = userIdentityEntity.serviceUserId!!,
                nickname = "랜덤닉네임${Random.nextInt()}", // TODO: 최초 닉네임 랜덤 생성 정책 정의 및 반영 필요
                termsAgreements = termsAgreementMap,
            )
        userProfileRepository.save(userProfileEntity)

        return userIdentityEntity.serviceUserId!!
    }

    @Transactional
    fun updateUser(serviceUserId: Long, customDeviceId: String, profile: Profile?, device: Device?) {
        if (profile != null) {
            val userProfileEntity = userProfileRepository.findByServiceUserIdAndDeletedAtIsNull(serviceUserId)
                ?: throw ServiceUserNotFoundException(serviceUserId)
            profile.nickname?.trim()?.takeIf { isAllowedNickname(it) && isExistingNickname(it).not() }!!.let { userProfileEntity.nickname = it }
            profile.imageUrl?.let { userProfileEntity.imageUrl = it }
            profile.district?.let { userProfileEntity.district = it }
            profile.interestFields?.let { userProfileEntity.interestFields = it }
            profile.interestLevel?.let { userProfileEntity.interestLevel = it }
            profile.termsAgreements?.let { userProfileEntity.termsAgreements = termsService.validateTermsAgreements(it) }
        }

        if (device != null) {
            deviceService.updateDevicePushToken(serviceUserId, customDeviceId, device.pushTokenType, device.pushToken)
        }
    }

    @Transactional
    fun deleteUser(serviceUserId: Long) {
        userIdentityRepository.findByIdOrNull(serviceUserId)?.apply {
            this.hashedCi = null
            this.getExistSocials().forEach { (type, uuid) ->
                this.setSocialUuid(type, null)
                socialService.deleteSocialUuid(uuid, type, true)
            }
            this.deletedAt = LocalDateTime.now()
        }

        userProfileRepository.markDeletedByServiceUserId(serviceUserId)

        deviceService.deleteAllDevice(serviceUserId)
    }

    fun makeFirebaseCustomToken(serviceUserId: Long, customDeviceId: String, osType: OsType, deviceModel: String): String {
        val claims = mapOf(
            "customDeviceId" to customDeviceId,
            "osType" to osType.name,
            "deviceModel" to deviceModel,
        )
        return FirebaseAuth.getInstance().createCustomToken(serviceUserId.toString(), claims)
    }

    private fun makeVoterType(isProfileCompleted: Boolean, isForeigner: Boolean, birthdate: LocalDate): VoterType {
        if (!isProfileCompleted)
            return VoterType.INCOMPLETE

        if (isForeigner)
            return VoterType.FOREIGNER

        val age = Period.between(birthdate, LocalDate.now()).years
        if (age < 18)
            return VoterType.UNDERAGE

        return VoterType.ELIGIBLE
    }
}
