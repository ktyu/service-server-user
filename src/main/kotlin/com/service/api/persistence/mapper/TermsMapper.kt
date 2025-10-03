package com.service.api.persistence.mapper

import com.service.api.model.Terms
import com.service.api.persistence.entity.JpaTermsEntity

object TermsMapper {
    fun toEntity(model: Terms): JpaTermsEntity =
        JpaTermsEntity(
            termsKey = model.termsKey,
            version = model.version,
            displayOrder = model.displayOrder,
            isMandatory = model.isMandatory,
            title = model.title,
            content = model.content,
            contentLink = model.contentLink,
            // createdAt, updatedAt 은 엔티티 기본값(LocalDateTime.now()) 사용
        )

    fun toModel(entity: JpaTermsEntity): Terms =
        Terms(
            termsKey = entity.termsKey,
            version = entity.version,
            displayOrder = entity.displayOrder,
            isMandatory = entity.isMandatory,
            title = entity.title,
            content = entity.content,
            contentLink = entity.contentLink,
        )
}
