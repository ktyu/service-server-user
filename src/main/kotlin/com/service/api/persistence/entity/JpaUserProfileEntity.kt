package com.service.api.persistence.entity

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.service.api.common.enum.District
import com.service.api.common.enum.InterestLevel
import com.service.api.common.InterestField
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_profile")
class JpaUserProfileEntity(
    @Id
    @Column(name = "service_user_id", nullable = false)
    val serviceUserId: Long,

    @Column(name = "nickname", nullable = false, unique = true, length = 64)
    var nickname: String,

    @Convert(converter = TermsAgreementMapJsonConverter::class)
    @Column(name = "terms_agreements", nullable = false, columnDefinition = "JSON")
    var termsAgreements: Map<String, Int>,

    @Column(name = "image_url", length = 256)
    var imageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "district", length = 16)
    var district: District? = null,

    @Convert(converter = InterestFieldSetJsonConverter::class)
    @Column(name = "interest_fields", columnDefinition = "JSON")
    var interestFields: Set<InterestField>? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_level", length = 8)
    var interestLevel: InterestLevel? = null,

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
)

@Converter
class InterestFieldSetJsonConverter : AttributeConverter<Set<InterestField>?, String?> {
    private val objectMapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: Set<InterestField>?): String? =
        attribute?.let { objectMapper.writeValueAsString(it.map { field -> field.value }) }

    override fun convertToEntityAttribute(dbData: String?): Set<InterestField>? =
        dbData?.let {
            objectMapper.readValue(it, object : TypeReference<List<String>>() {}).map { value -> InterestField(value) }.toSet()
        }
    }

@Converter
class TermsAgreementMapJsonConverter : AttributeConverter<Map<String, Int>, String> {
    private val objectMapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<String, Int>?): String? =
        attribute?.let { objectMapper.writeValueAsString(it) }

    override fun convertToEntityAttribute(dbData: String?): Map<String, Int>? =
        dbData?.let {
            objectMapper.readValue(it, objectMapper.typeFactory.constructMapType(Map::class.java, String::class.java, Int::class.javaObjectType))
        }
}
