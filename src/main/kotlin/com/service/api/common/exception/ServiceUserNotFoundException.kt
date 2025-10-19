package com.service.api.common.exception

class ServiceUserNotFoundException(serviceUserId: Long): RuntimeException("serviceUserId not found: $serviceUserId") {
}
