package com.service.api.service

import com.service.api.common.AgeGroup
import com.service.api.common.enum.GenderType
import com.service.api.common.enum.SocialType
import com.service.api.common.enum.VoterType
import com.service.api.common.exception.AlreadySignupCiException
import com.service.api.common.exception.AlreadySignupCiInfo
import com.service.api.model.TermsAgreement
import com.service.api.model.User
import com.service.api.persistence.entity.JpaUserIdentityEntity
import com.service.api.persistence.entity.JpaUserProfileEntity
import com.service.api.persistence.mapper.DeviceMapper
import com.service.api.persistence.mapper.ProfileMapper
import com.service.api.persistence.repository.*
import com.service.api.service.social.SocialService
import com.service.api.util.Sha256HashingUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Period
import kotlin.random.Random

@Service
class UserService(
    private val termsService: TermsService,
    private val deviceService: DeviceService,
    private val socialService: SocialService,
    private val userSocialRepository: UserSocialRepository,
    private val userIdentityRepository: UserIdentityRepository,
    private val userProfileRepository: UserProfileRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getUserWithDevice(serviceUserId: Long, customDeviceId: String): User {
        val projection = userIdentityRepository.findUserProjectionByServiceUserIdAndCustomDeviceIdAndDeletedAtIsNull(serviceUserId, customDeviceId)
        val identity = projection.identity
        val profile = projection.profile
        val device = projection.device

        val isProfileCompleted = profile.district != null && profile.interestFields != null && profile.interestLevel != null
        val voterType = makeVoterType(isProfileCompleted, identity.isForeigner, identity.birthdate)

        val socialTypes = buildList {
            if (identity.kakaoSub != null) add(SocialType.KAKAO)
            if (identity.appleSub != null) add(SocialType.APPLE)
            if (identity.naverSub != null) add(SocialType.NAVER)
            if (identity.googleSub != null) add(SocialType.GOOGLE)
        }

        return User(
            profile = ProfileMapper.toModel(profile), // TODO: 이메일 정보 넣어줘야함
            device = DeviceMapper.toModel(device),
            voterType = voterType,
            ageGroup = AgeGroup.fromBirthdate(identity.birthdate),
            socialTypes = socialTypes
        )
    }

    @Transactional
    fun createUser(termsAgreements: List<TermsAgreement>, socialType: SocialType, socialUuid: String, identityToken: String): Long {
        // 약관 동의 상태 검사
        val termsAgreementMap = termsService.validateTermsAgreements(termsAgreements)

        // 소셜 연결 상태 검증
        val sub = socialService.getSubWithValidation(socialType, socialUuid)

        // 본인인증 결과 받아서 가공
        val hashedCi = Sha256HashingUtil.sha256Hex(identityToken, "service")
        val mobilePhoneNumber = "01012345678"
        val isForeigner = false
        val gender = GenderType.M
        val birthdate = LocalDate.now()
        // TODO: MDL_TKN(identityToken) 값 보내서 본인인증 결과 받아오기

        // 이미 가입되어 있는 CI 라면, 기존 사용자 정보와 함께 208 리턴
        userIdentityRepository.findByHashedCiAndDeletedAtIsNull(hashedCi)?.run {
            val userSocialEntity = mapOf(
                SocialType.KAKAO to kakaoSub,
                SocialType.APPLE to appleSub,
                SocialType.NAVER to naverSub,
                SocialType.GOOGLE to googleSub,
            ).entries.first { it.value != null }.let { existUserSocial ->
                userSocialRepository.findBySocialTypeAndSub(existUserSocial.key, existUserSocial.value!!)
            }

            throw AlreadySignupCiException(
                AlreadySignupCiInfo(
                    serviceUserId = serviceUserId!!,
                    socialType = socialType,
                    email = userSocialEntity?.email ?: "",
                    lastLoginDate = deviceService.getLastLoginDateByServiceUserId(serviceUserId!!)!!,
                )
            )
        }

        // 가입 처리
        val userIdentityEntity = userIdentityRepository.save(JpaUserIdentityEntity(
            hashedCi = hashedCi,
            mobilePhoneNumber = mobilePhoneNumber,
            isForeigner = isForeigner,
            gender = gender,
            birthdate = birthdate,
            kakaoSub = if (socialType == SocialType.KAKAO) sub else null,
            appleSub = if (socialType == SocialType.APPLE) sub else null,
            naverSub = if (socialType == SocialType.NAVER) sub else null,
            googleSub = if (socialType == SocialType.GOOGLE) sub else null,
        ))
        userProfileRepository.save(JpaUserProfileEntity(
            serviceUserId = userIdentityEntity.serviceUserId!!,
            nickname = "랜덤닉네임${Random.nextInt()}", // TODO: 최초 닉네임 랜덤 생성 정책 정의 및 반영 필요
            termsAgreements = termsAgreementMap,
        ))

        return userIdentityEntity.serviceUserId!!
    }

    @Transactional
    fun deleteUser(serviceUserId: Long) {
        userIdentityRepository.markDeletedByServiceUserId(serviceUserId)
        userProfileRepository.markDeletedByServiceUserId(serviceUserId)
        deviceService.deleteAllDevice(serviceUserId)
        // TODO: 일단 성공 리턴하고 백그라운드로 연동된 소셜들 연동끊기 요청하고 userSocial 엔티티도 deletedAt 찍기
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
