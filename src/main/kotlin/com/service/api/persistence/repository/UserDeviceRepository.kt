package com.service.api.persistence.repository

import com.service.api.persistence.entity.JpaUserDeviceEntity
import com.service.api.persistence.entity.UserDeviceId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserDeviceRepository : JpaRepository<JpaUserDeviceEntity, UserDeviceId> {

    fun findByIdAndDeletedAtIsNull(id: UserDeviceId): JpaUserDeviceEntity?

    fun findAllById_ServiceUserId(serviceUserId: Long): List<JpaUserDeviceEntity>

    @Modifying
    @Query("UPDATE JpaUserDeviceEntity d SET d.deletedAt = CURRENT_TIMESTAMP WHERE d.id.serviceUserId = :serviceUserId AND d.id.customDeviceId = :customDeviceId")
    fun markDeletedByServiceUserIdAndCustomDeviceId(serviceUserId: Long, customDeviceId: String)

    @Modifying
    @Query("UPDATE JpaUserDeviceEntity d SET d.deletedAt = CURRENT_TIMESTAMP WHERE d.id.serviceUserId = :serviceUserId")
    fun markDeletedByServiceUserId(serviceUserId: Long)
}
