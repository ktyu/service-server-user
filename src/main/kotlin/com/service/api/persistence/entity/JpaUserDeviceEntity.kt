package com.service.api.persistence.entity

import com.service.api.common.enum.OsType
import com.service.api.common.enum.PushTokenType
import jakarta.persistence.*
import java.io.Serializable
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_device"
)
class JpaUserDeviceEntity(
    @EmbeddedId
    val id: UserDeviceId,

    @Column(name = "device_model", nullable = false, length = 32)
    var deviceModel: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "os_type", nullable = false, length = 8)
    var osType: OsType,

    @Column(name = "os_version", nullable = false, length = 32)
    var osVersion: String,

    @Column(name = "app_version", nullable = false, length = 8)
    var appVersion: String,

    @Column(name = "access_token_issued_at", nullable = false)
    var accessTokenIssuedAt: Long = -1,

    @Column(name = "refresh_token_issued_at", nullable = false)
    var refreshTokenIssuedAt: Long = -1,

    @Column(name = "last_login_at", nullable = false)
    var lastLoginAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "push_token_type", nullable = false, length = 32)
    var pushTokenType: PushTokenType,

    @Column(name = "push_token", nullable = false, length = 1024)
    var pushToken: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
)

@Embeddable
data class UserDeviceId(
    @Column(name = "service_user_id")
    val serviceUserId: Long,

    @Column(name = "custom_device_id", columnDefinition = "CHAR(36)")
    val customDeviceId: String
) : Serializable
