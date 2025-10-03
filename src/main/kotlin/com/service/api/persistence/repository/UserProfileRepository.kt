package com.service.api.persistence.repository

import com.service.api.persistence.entity.JpaUserProfileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserProfileRepository : JpaRepository<JpaUserProfileEntity, Long> {

}
