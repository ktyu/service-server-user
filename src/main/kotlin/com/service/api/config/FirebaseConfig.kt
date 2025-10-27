package com.service.api.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class FirebaseConfig(
    @Value("\${firebase.credentials}") private val firebaseCredentials: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        if (FirebaseApp.getApps().isEmpty()) {
            val decodedJson = String(Base64.getDecoder().decode(firebaseCredentials.trim()))
            val credentialsStream = decodedJson.byteInputStream()
            try {
                FirebaseApp.initializeApp(
                    FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                        .build()
                )
            } catch (e: Exception) {
                log.error("fail to initialize FirebaseApp...")
            }
        }
    }
}
