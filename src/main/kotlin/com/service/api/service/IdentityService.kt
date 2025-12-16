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
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.net.InetAddress
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class IdentityService(
    private val userIdentityRepository: UserIdentityRepository,
    private val userIdentityMappingRepository: UserIdentityMappingRepository,

    @Value("\${identity.salt-for-ci-hashing}") private val saltForCiHashing: String,
    @Value("\${identity.kcb.cp-id}") private val kcbCpId: String,
    @Value("\${identity.kcb.license:}") private val kcbLicense: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isKcbLicenseLoaded(): Boolean = kcbLicense.isNotBlank()

    fun requestKcbPhoneIdentityVerification(returnUrl: String): String {
        val reqJson = mapOf(
            "RETURN_URL" to returnUrl, // KCB 인증이 완료된 후 리턴될 회원사 페이지 - 도메인 포함 full path
            "SITE_NAME" to "내 서비스", // 요청 사이트명 (최대 24 Bytes – LGU 제한사항으로 최대길이엄수)
            "SITE_URL" to "www.service.co.kr", //사이트의 URL (추후 사용자가 이력을 확인할 때 보여질 수 있는 URL)
            "RQST_CAUS_CD" to "00", // 인증요청 사유코드 - 00 : 회원가입 / 01 : 성인인증 / 02 : 회원정보수정 / 03 : 비밀번호찾기 / 04 : 상품구매 / 99 : 기타
        )

        val resultStr = ByteArrayInputStream(Base64.getDecoder().decode(kcbLicense.trim())).use {
            //kcb.module.v3.OkCert().callOkCert("PROD", kcbCpId, "IDS_HS_POPUP_START", null,  reqJson.toString(), it)
        }
        val resJson = mapOf<String, String>()//kcb.org.json.JSONObject(resultStr)

        val resultCode: String? = resJson["RSLT_CD"]
        val resultMsg: String? = resJson["RSLT_MSG"]
        val txSeqNo: String? = resJson["TX_SEQ_NO"]
        val mdlTkn: String? = resJson["MDL_TKN"]

        if (resultCode != "B000" || mdlTkn.isNullOrBlank()) {
            throw RuntimeException("KCB Phone Identity Verification Request(returnUrl=$returnUrl, hostname=${InetAddress.getLocalHost().hostName}, txSeqNo=$txSeqNo, mdlTkn=$mdlTkn) failed: $resultCode($resultMsg)")
        } else {
            log.info("KCB Phone Identity Verification Request(returnUrl=$returnUrl, hostname=${InetAddress.getLocalHost().hostName}, txSeqNo=$txSeqNo, mdlTkn=$mdlTkn) ok: $resultCode($resultMsg)")
        }

        return mdlTkn
    }

    private fun getIdentityInfoFromKcb(token: String): IdentityInfo {
        val reqJson = mapOf(
            "MDL_TKN" to token,
        )

        val resultStr = ByteArrayInputStream(Base64.getDecoder().decode(kcbLicense.trim())).use {
            //kcb.module.v3.OkCert().callOkCert("PROD", kcbCpId, "IDS_HS_POPUP_RESULT", null,  reqJson.toString(), it)
        }
        val resJson = mapOf<String, String>()//kcb.org.json.JSONObject(resultStr)

        val resultCode: String? = resJson["RSLT_CD"]
        val resultMsg: String? = resJson["RSLT_MSG"]
        val txSeqNo: String? = resJson["TX_SEQ_NO"]

        if (resultCode != "B000") {
            throw RuntimeException("KCB Phone Identity Verification Result(hostname=${InetAddress.getLocalHost().hostName}, txSeqNo=$txSeqNo, token=$token) failed: $resultCode($resultMsg)")
        } else {
            log.info("KCB Phone Identity Verification Result(hostname=${InetAddress.getLocalHost().hostName}, txSeqNo=$txSeqNo, token=$token) ok: $resultCode($resultMsg), {}", resJson)
        }
        resJson["CI_UPDATE"].also {
            if (it != "1") throw RuntimeException("ciUpdate is not 1: $it")
        }

        val ci = resJson["CI"]!!
        val resultBirthday = resJson["RSLT_BIRTHDAY"]!!.also {
            if (it.length != 8) throw RuntimeException("Unknown RSLT_BIRTHDAY: $it")
        }
        val resultSexCode = resJson["RSLT_SEX_CD"]!!.also {
            if (it != "M" && it != "F") throw RuntimeException("Unknown RSLT_SEX_CD: $it")
        }
        val resultNativeForeignerCode = resJson["RSLT_NTV_FRNR_CD"]!!.also {
            if (it != "L" && it != "F") throw RuntimeException("Unknown RSLT_NTV_FRNR_CD: $it")
        }

        return IdentityInfo(
            hashedCi = Sha256HashingUtil.sha256Hex(ci, saltForCiHashing, Sha256HashingUtil.SaltMode.PREFIX),
            isForeigner = resultNativeForeignerCode == "F",
            gender = GenderType.valueOf(resultSexCode),
            birthdate = LocalDate.parse(resultBirthday, DateTimeFormatter.ofPattern("yyyyMMdd")),
        )
    }

    fun getIdentityIdBy(serviceUserId: Long): Long? {
        return userIdentityMappingRepository.findByIdServiceUserIdAndDeletedAtIsNull(serviceUserId)?.id?.identityId
    }

    @Transactional
    fun saveIdentity(mdlTkn: String, serviceUserId: Long, socialId: Long): Pair<Long, AgeGroup> {
        val identityInfo = if (mdlTkn.startsWith("_") && !isKcbLicenseLoaded()) { // mock 에서 생성한 토큰
            getIdentityInfoFromMock(mdlTkn.drop(1))
        } else { // 실제 kcb 에서 생성한 토큰
            getIdentityInfoFromKcb(mdlTkn)
        }

        // 본인인증 정보 저장
        var userIdentityEntity = userIdentityRepository.findByHashedCi(identityInfo.hashedCi)
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
                hashedCi = identityInfo.hashedCi,
                isForeigner = identityInfo.isForeigner,
                gender = identityInfo.gender,
                birthdate = identityInfo.birthdate,
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
            Pair(serviceUserId, AgeGroup.fromBirthdate(identityInfo.birthdate))
        } else if (existingMappedServiceUserId != serviceUserId) {
            // 계정 병합 필요
            Pair(existingMappedServiceUserId, AgeGroup.fromBirthdate(identityInfo.birthdate))
        } else {
            log.warn("duplicate identity mapping is saving: ${userIdentityEntity.identityId}")
            Pair(serviceUserId, AgeGroup.fromBirthdate(identityInfo.birthdate))
        }
    }

    @Transactional
    fun deleteIdentity(identityId: Long) {
        userIdentityMappingRepository.markDeletedByIdentityId(identityId)

        userIdentityRepository.markDeletedByIdentityId(identityId)
    }

    private fun getIdentityInfoFromMock(token: String): IdentityInfo {
        return try {
            val num = String(Base64.getUrlDecoder().decode(token)).split("@")[1].split("-")
            if ((num[1].toInt() in 1..8).not()) throw RuntimeException()

            IdentityInfo(
                hashedCi = Sha256HashingUtil.sha256Hex(token, saltForCiHashing, Sha256HashingUtil.SaltMode.PREFIX),
                isForeigner = num[1].toInt() >= 5, // 뒷자리가 5~8 면 외국인
                gender = if (num[1].toInt() % 2 == 0) GenderType.F else GenderType.M,
                birthdate = LocalDate.parse("${if (num[1].toInt() <= 2) 19 else 20}${num[0]}", DateTimeFormatter.ofPattern("yyyyMMdd")),
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("invalid token from mock: $token")
        }
    }

}

data class IdentityInfo(
    val hashedCi: String,
    val isForeigner: Boolean,
    val gender: GenderType,
    val birthdate: LocalDate,
)
