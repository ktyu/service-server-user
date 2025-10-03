package com.service.api.config

import com.service.api.filter.ApiHeaderContextFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilterConfig {

    @Bean
    fun apiHeaderContextFilterRegistration(): FilterRegistrationBean<ApiHeaderContextFilter> {
        return FilterRegistrationBean<ApiHeaderContextFilter>().apply {
            filter = ApiHeaderContextFilter()
            order = 1
            addUrlPatterns("/api/*")
        }
    }
}
