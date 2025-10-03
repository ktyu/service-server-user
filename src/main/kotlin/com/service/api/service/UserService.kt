package com.service.api.service

import com.service.api.common.enum.GenderType
import com.service.api.common.enum.SocialType
import com.service.api.model.TermsAgreement
import com.service.api.persistence.entity.JpaUserIdentityEntity
import com.service.api.persistence.entity.JpaUserProfileEntity
import com.service.api.persistence.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class UserService(
    private val termsService: TermsService,
    private val socialKakaoService: SocialKakaoService,
    private val userIdentityRepository: UserIdentityRepository,
    private val userProfileRepository: UserProfileRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createUser(termsAgreements: List<TermsAgreement>, socialType: SocialType, socialUuid: String, identityToken: String): Long {
        // 약관 동의 상태 검사
        val termsAgreementMap = termsService.validateTermsAgreements(termsAgreements)

        // 소셜 연동 상태 검증
        val sub = when (socialType) {
            SocialType.KAKAO -> socialKakaoService.getSubWithExternalValidation(socialUuid)
            SocialType.APPLE,
            SocialType.NAVER,
            SocialType.GOOGLE -> TODO()
        }

        // 본인인증 정보 교환
        val hashedCi = identityToken
        // TODO: identityToken 을 교환해서 결과 받아오고 해싱 처리 등..

        // 이미 가입되어 있는 CI 라면, 기존 사용자 정보와 함께 205 리턴
        userIdentityRepository.findByHashedCiAndDeletedAtIsNull(hashedCi)?.let { userIdentityEntity ->
            throw RuntimeException("already exist ci")// TODO: 기존 사용자 정보와 함께 205 리턴
        }

        // 가입 처리
        val userIdentityEntity = userIdentityRepository.save(JpaUserIdentityEntity(
            hashedCi = hashedCi, // TODO: 본인인증 받은 정보들로 채워야함
            mobilePhoneNumber = "01012345678",
            isForeigner = false,
            gender = GenderType.M,
            birthdate = LocalDate.now(),
            kakaoSub = if (socialType == SocialType.KAKAO) sub else null,
            appleSub = if (socialType == SocialType.APPLE) sub else null,
            naverSub = if (socialType == SocialType.NAVER) sub else null,
            googleSub = if (socialType == SocialType.GOOGLE) sub else null,
        ))
        userProfileRepository.save(JpaUserProfileEntity(
            serviceUserId = userIdentityEntity.serviceUserId!!,
            nickname = "랜덤-$hashedCi",
            termsAgreements = termsAgreementMap,
        ))

        return userIdentityEntity.serviceUserId!!
    }
}
