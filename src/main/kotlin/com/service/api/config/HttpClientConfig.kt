package com.service.api.config

import com.service.api.client.KapiKakaoComClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import reactor.core.publisher.Mono

@Configuration
class HttpClientConfig {
    private val logger = LoggerFactory.getLogger(HttpClientConfig::class.java)

    @Bean
    fun kapiKakaoComWebClient(
        @Value("\${api.kapi-kakao-com.base-url}") baseUrl: String,
    ): WebClient =
        WebClient
            .builder()
            .baseUrl(baseUrl)
            .filter(logRequest())
            .filter(logResponse())
            .build()

    @Bean
    fun kapiKakaoComClient(kapiKakaoComWebClient: WebClient): KapiKakaoComClient {
        val factory =
            HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(kapiKakaoComWebClient))
                .build()
        return factory.createClient(KapiKakaoComClient::class.java)
    }

    private fun logRequest(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofRequestProcessor { request ->
            logger.info("📤 HTTP 요청: {} {}", request.method(), request.url())
            logger.info("📤 요청 헤더: {}", request.headers())
            Mono.just(request)
        }

    private fun logResponse(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofResponseProcessor { response ->
            logger.info("📥 HTTP 응답: {} {}", response.statusCode(), response.request().uri)
            logger.info("📥 응답 헤더: {}", response.headers().asHttpHeaders())
            Mono.just(response)
        }
}
