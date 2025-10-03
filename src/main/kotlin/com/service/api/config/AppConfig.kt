package com.service.api.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@ComponentScan(
    basePackages = [
        "com.service.api.controller",
        "com.service.api.service",
        "com.service.api.persistence",
        "com.service.api.client",
        "com.service.api.config",
    ],
)
@EnableJpaRepositories(basePackages = ["com.service.api.persistence.repository"])
@EntityScan(basePackages = ["com.service.api.persistence.entity"])
class AppConfig
