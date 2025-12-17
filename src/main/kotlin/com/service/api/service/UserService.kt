package com.service.api.service

import com.google.firebase.auth.FirebaseAuth
import com.service.api.common.enum.AgeGroup
import com.service.api.common.enum.Region
import com.service.api.common.enum.OsType
import com.service.api.common.enum.VoterType
import com.service.api.common.exception.ServiceUserNotFoundException
import com.service.api.model.*
import com.service.api.persistence.entity.JpaUserProfileEntity
import com.service.api.persistence.repository.*
import com.service.api.persistence.mapper.DeviceMapper
import com.service.api.persistence.mapper.ProfileMapper
import com.service.api.service.social.SocialService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Service
class UserService(
    private val termsService: TermsService,
    private val deviceService: DeviceService,
    private val identityService: IdentityService,
    private val socialService: SocialService,
    private val userProfileRepository: UserProfileRepository,
    private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getUserWithDevice(serviceUserId: Long, socialId: Long, customDeviceId: String): User {
        val projection = userRepository.findUserProjectionBy(serviceUserId = serviceUserId, customDeviceId = customDeviceId)
            ?: throw ServiceUserNotFoundException(serviceUserId)
        with (projection) {
            val voteEligibility = UserVoteEligibility(
                region = profile.region,
                interestFields = profile.interestFields,
                interestLevel = profile.interestLevel,
                issueNote = profile.issueNote,
                isForeigner = identity?.isForeigner,
                birthdate = identity?.birthdate,
            )

            val socialAccounts = getUserSocialAccounts(serviceUserId, socialId)

            return User(
                profile = ProfileMapper.toModel(profile),
                device = device?.let { DeviceMapper.toModel(it) },
                voterType = UserVoteEligibility.makeVoterType(voteEligibility),
                ageGroup = AgeGroup.fromBirthdate(identity?.birthdate),
                genderType = identity?.gender,
                socialAccounts = socialAccounts,
            )
        }
    }

    fun isExistingNickname(nickname: String): Boolean {
        return userProfileRepository.existsByNickname(nickname)
    }

    fun isAllowedNickname(nickname: String): Boolean {
        return nickname.trim() !in listOf("관리자", "운영자") // TODO: 정확한 정책 정해지면 구현 필요
    }

    fun getRegionAndAgeGroupIfEligibleOrNull(serviceUserId: Long): Pair<Region, AgeGroup>? {
        userRepository.findUserVoteEligibilityBy(serviceUserId)?.let {
            if (UserVoteEligibility.makeVoterType(it) == VoterType.ELIGIBLE)
                return Pair(it.region!!, AgeGroup.fromBirthdate(it.birthdate!!))
        }
        return null
    }

    @Transactional
    fun createUser(termsAgreements: List<TermsAgreement>, socialId: Long): Long {
        // 약관 동의 상태 검사
        val termsAgreementMap = termsService.validateTermsAgreements(termsAgreements)

        // 소셜 계정 사용 여부 검사
        if (socialService.getServiceUserIdBy(socialId) != null) {
            throw IllegalArgumentException("already using social account! should use login API")
        }

        // 유저 생성
        val userProfileEntity = userProfileRepository.save(JpaUserProfileEntity(
            nickname = "랜덤닉네임${Random.nextInt()}", // TODO: 최초 닉네임 랜덤 생성 정책 정의 및 반영 필요
            termsAgreements = termsAgreementMap,
        ))

        // 유저-소셜 매핑 정보 저장
        socialService.saveUserSocialMapping(userProfileEntity.serviceUserId!!, socialId)

        return userProfileEntity.serviceUserId!!
    }

    @Transactional
    fun updateUser(serviceUserId: Long, customDeviceId: String, profile: Profile?, device: Device?) {
        if (profile != null) {
            val userProfileEntity = userProfileRepository.findByServiceUserIdAndDeletedAtIsNull(serviceUserId)
                ?: throw ServiceUserNotFoundException(serviceUserId)
            profile.nickname?.trim()?.let {
                if (!isAllowedNickname(it) || isExistingNickname(it))
                    throw IllegalArgumentException("check nickname availability")
                userProfileEntity.nickname = it
                userProfileEntity.nicknameUpdatedAt = LocalDateTime.now()
            }
            profile.imageUrl?.let { userProfileEntity.imageUrl = it }
            profile.region?.let { userProfileEntity.region = it }
            profile.interestFields?.let { userProfileEntity.interestFields = it }
            profile.interestLevel?.let { userProfileEntity.interestLevel = it }
            profile.termsAgreements?.let {
                userProfileEntity.termsAgreements = termsService.validateTermsAgreements(it)
                userProfileEntity.termsAgreementsUpdatedAt = LocalDateTime.now()
            }
        }

        if (device != null) {
            deviceService.updateDevicePushToken(serviceUserId, customDeviceId, device.pushTokenType, device.pushToken)
        }
    }

    @Transactional
    fun mergeUser(sourceServiceUserId: Long, socialId: Long, targetServiceUserId: Long): List<SocialAccount> {
        // source profile 은 삭제하고, 닉네임 & 약관동의 이력을 target profile 과 병합
        val sourceProfile = userProfileRepository.findByServiceUserIdAndDeletedAtIsNull(sourceServiceUserId)
            ?: throw ServiceUserNotFoundException(sourceServiceUserId)
        userProfileRepository.delete(sourceProfile)

        val targetProfile = userProfileRepository.findByServiceUserIdAndDeletedAtIsNull(targetServiceUserId)
            ?: throw ServiceUserNotFoundException(sourceServiceUserId)
        if (sourceProfile.nicknameUpdatedAt.isAfter(targetProfile.nicknameUpdatedAt)) {
            targetProfile.nickname = sourceProfile.nickname
            targetProfile.nicknameUpdatedAt = sourceProfile.nicknameUpdatedAt
        }
        targetProfile.termsAgreements = termsService.mergeTermsAgreements(sourceProfile.termsAgreements, targetProfile.termsAgreements)
        targetProfile.termsAgreementsUpdatedAt = maxOf(sourceProfile.termsAgreementsUpdatedAt, targetProfile.termsAgreementsUpdatedAt)

        // source 의 device 들을 target 으로 이관
        deviceService.mergeAllDeviceServiceUserId(sourceServiceUserId, targetServiceUserId)

        // source 의 social mapping 들을 target 으로 이관
        socialService.mergeAllSocialMappingServiceUserId(sourceServiceUserId, targetServiceUserId)

        // 병합 완료 이후, 현재 소셜 아이디 정보들 리턴
        return getUserSocialAccounts(targetServiceUserId, socialId)
    }

    @Transactional
    fun deleteUser(serviceUserId: Long) {
        userProfileRepository.markDeletedByServiceUserId(serviceUserId)

        deviceService.deleteAllDevice(serviceUserId)

        identityService.getIdentityIdBy(serviceUserId = serviceUserId)?.let { identityId ->
            identityService.deleteIdentity(identityId = identityId)
        }

        userRepository.findSocialAccountsBy(serviceUserId = serviceUserId).forEach { socialAccount ->
            socialService.deleteAndRevokeSocialStatus(socialId = socialAccount.id)
        }
    }

    fun makeFirebaseCustomToken(serviceUserId: Long, customDeviceId: String, osType: OsType, deviceModel: String): String {
        val claims = mapOf(
            "customDeviceId" to customDeviceId,
            "osType" to osType.name,
            "deviceModel" to deviceModel,
        )
        return FirebaseAuth.getInstance().createCustomToken(serviceUserId.toString(), claims)
    }

    private fun getUserSocialAccounts(serviceUserId: Long, currentSocialId: Long): List<SocialAccount> {
        return userRepository.findSocialAccountsBy(serviceUserId = serviceUserId).run {
            val current = firstOrNull { it.id == currentSocialId }
                ?: throw RuntimeException("currentSocialId is missing in socialAccounts: $currentSocialId")
            listOf(current) + filterNot { it == current } // 현재 로그인된 소셜 계정을 첫 번째 원소로 둠
        }
    }
}
