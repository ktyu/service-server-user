package com.service.api.persistence.repository

import com.service.api.persistence.entity.JpaTermsEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TermsRepository : JpaRepository<JpaTermsEntity, Int> {

}
