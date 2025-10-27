package com.service.api.persistence.mapper

import com.service.api.model.Terms
import com.service.api.persistence.entity.JpaTermsEntity

object TermsMapper {
    fun toModel(entity: JpaTermsEntity) =
        Terms(
            termsKey = entity.termsKey,
            version = entity.version,
            displayOrder = entity.displayOrder,
            isMandatory = entity.isMandatory,
            title = entity.title,
            contentLink = entity.contentLink,
        )
}
