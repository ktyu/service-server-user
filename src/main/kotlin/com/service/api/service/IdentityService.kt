package com.service.api.service

import com.service.api.common.enum.AgeGroup
import com.service.api.common.enum.GenderType
import com.service.api.persistence.entity.JpaUserIdentityEntity
import com.service.api.persistence.entity.JpaUserIdentityMappingEntity
import com.service.api.persistence.entity.UserIdentityMappingId
import com.service.api.persistence.repository.UserIdentityMappingRepository
import com.service.api.persistence.repository.UserIdentityRepository
import com.service.api.util.Sha256HashingUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class IdentityService(
    private val userIdentityRepository: UserIdentityRepository,
    private val userIdentityMappingRepository: UserIdentityMappingRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getIdentityIdBy(serviceUserId: Long): Long? {
        return userIdentityMappingRepository.findByIdServiceUserIdAndDeletedAtIsNull(serviceUserId)?.id?.identityId
    }

    @Transactional
    fun saveIdentity(identityToken: String, serviceUserId: Long, socialId: Long): Pair<Long, AgeGroup> {
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

        // 본인인증 정보 저장
        var userIdentityEntity = userIdentityRepository.findByHashedCi(hashedCi)
            ?.apply {
                // 이미 저장된 CI 일 경우 정보 비교
                log.warn("duplicate hashed ci is saving: $hashedCi")
                if (this.deletedAt != null)
                    throw RuntimeException("should not happened. deleted identity still has hashedCi")
                if (this.isForeigner != isForeigner || this.gender != gender || this.birthdate != birthdate) {
                    // CI는 그대로인데 본인인증 결과 정보가 달라졌으면 에러 로깅만 남김 (실제 발생은 안할 것으로 기대)
                    log.error("permanent info changed... {}/{}, {}/{}, {}/{}",
                        this.isForeigner, isForeigner, this.gender, gender, this.birthdate, birthdate)
                }
            }
            ?: JpaUserIdentityEntity(
                // 저장되지 않은 CI 일 경우 신규 identityId 로 저장
                hashedCi = hashedCi,
                isForeigner = isForeigner,
                gender = gender,
                birthdate = birthdate,
            )
        userIdentityEntity = userIdentityRepository.save(userIdentityEntity)

        // 유저-본인인증 매핑 정보 저장
        val existingMappedServiceUserId = userIdentityMappingRepository.findByIdIdentityIdAndDeletedAtIsNull(userIdentityEntity.identityId!!)?.id?.serviceUserId
        return if (existingMappedServiceUserId == null) {
            // 신규 매핑
            userIdentityMappingRepository.save(
                JpaUserIdentityMappingEntity(UserIdentityMappingId(
                    serviceUserId = serviceUserId,
                    identityId = userIdentityEntity.identityId!!,
                ))
            )
            Pair(serviceUserId, AgeGroup.fromBirthdate(birthdate))
        } else if (existingMappedServiceUserId != serviceUserId) {
            // 계정 병합 필요
            Pair(existingMappedServiceUserId, AgeGroup.fromBirthdate(birthdate))
        } else {
            log.warn("duplicate identity mapping is saving: ${userIdentityEntity.identityId}")
            Pair(serviceUserId, AgeGroup.fromBirthdate(birthdate))
        }
    }

    @Transactional
    fun deleteIdentity(identityId: Long) {
        userIdentityMappingRepository.markDeletedByIdentityId(identityId)

        userIdentityRepository.markDeletedByIdentityId(identityId)
    }
}
