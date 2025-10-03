package com.service.api.persistence.repository

import com.service.api.persistence.entity.JpaUserDeviceEntity
import com.service.api.persistence.entity.UserDeviceId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserDeviceRepository : JpaRepository<JpaUserDeviceEntity, UserDeviceId> {

    fun findByIdAndDeletedAtIsNull(id: UserDeviceId): JpaUserDeviceEntity?
}
