package com.service.api.persistence.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "terms")
data class JpaTermsEntity(
    @Id
    @Column(name = "terms_key", nullable = false)
    val termsKey: String,

    @Column(name = "version", nullable = false)
    val version: Int,

    @Column(name = "display_order", nullable = false, unique = true)
    val displayOrder: Int,

    @Column(name = "is_mandatory", nullable = false)
    val isMandatory: Boolean,

    @Column(name = "title", nullable = false, length = 64)
    val title: String,

    @Column(name = "content_link", nullable = false, length = 256)
    val contentLink: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
