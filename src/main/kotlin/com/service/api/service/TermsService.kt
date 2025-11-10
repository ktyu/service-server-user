package com.service.api.service

import com.service.api.common.exception.TermsAgreementException
import com.service.api.model.Terms
import com.service.api.model.TermsAgreement
import com.service.api.persistence.mapper.TermsMapper
import com.service.api.persistence.repository.TermsRepository
import org.springframework.stereotype.Service

@Service
class TermsService(
    private val termsRepository: TermsRepository,
) {

    fun getAllTerms(): List<Terms> {
        return termsRepository.findAll().map { entity ->
            TermsMapper.toModel(entity)
        }
    }

    internal fun validateTermsAgreements(termsAgreements: List<TermsAgreement>): Map<String, Int> {
        val termsAgreementMap = termsAgreements.associate { it.termsKey to it.version }
        termsRepository.findAll()
            .filter { it.displayOrder >= 0 } // 미노출 중인 약관 제외
            .groupBy { it.isMandatory }
            .forEach { (isMandatory, termsList) ->
                if (isMandatory) {
                    // 모든 필수 약관에 동의하고, 버전이 최신값과 일치
                    if (!termsList.all { mandatoryTerms ->
                            termsAgreementMap[mandatoryTerms.termsKey] == mandatoryTerms.version
                        })
                        throw TermsAgreementException("Mandatory terms agreement is missing or not latest")
                } else {
                    // 동의한 선택 약관이 존재한다면, 버전이 최신값과 일치
                    if (!termsList.all { nonMandatoryTerms ->
                            termsAgreementMap[nonMandatoryTerms.termsKey] == null
                                    || termsAgreementMap[nonMandatoryTerms.termsKey] == nonMandatoryTerms.version
                        })
                        throw TermsAgreementException("Non-Mandatory terms agreement is not latest")
                }
            }

        return termsAgreementMap
    }

    internal fun mergeTermsAgreements(m1: Map<String, Int>, m2: Map<String, Int>): Map<String, Int> {
        return m1.keys
            .intersect(m2.keys) // 두 맵 모두에 존재하는 키(termsKey)만 추출
            .associateWith { key ->
                maxOf(m1.getValue(key), m2.getValue(key)) // 각 키에 대해 더 큰 값(최신 version)을 선택
            }
    }
}
