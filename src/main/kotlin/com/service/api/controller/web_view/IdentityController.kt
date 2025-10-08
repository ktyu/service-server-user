package com.service.api.controller.web_view

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/web-view")
class IdentityController { // TODO: 전체 재구현

    @GetMapping("/identity")
    fun getIdentityPage(): ResponseEntity<Void> {
        return ResponseEntity
            .status(HttpStatus.FOUND) // 302
            .header(HttpHeaders.LOCATION, "/web-view/callback/identity")
            .build()
    }

    @GetMapping("/callback/identity")
    fun callbackIdentity(): ResponseEntity<String> {
        val targetUrl = "/web-view/identity/exit?isSuccess=true&MDL_TKN=${java.util.UUID.randomUUID().toString().replace("-", "")}"
        val html = """
            <html lang="ko">
              <head>
                <meta charset="UTF-8">
                <meta http-equiv="refresh" content="1;url=$targetUrl">
              </head>
              <body style="font-family:sans-serif; text-align:center; margin-top:20%;">
                <h2>본인인증(mock) 처리중... (1초 소요)</h2>
              </body>
            </html>
        """.trimIndent()
        return ResponseEntity.ok()
            .header("Content-Type", "text/html; charset=UTF-8")
            .body(html)
    }

    @GetMapping("/identity/exit")
    fun getIdentityExitPage(): ResponseEntity<Void> {
        return ResponseEntity.ok().build()
    }
}
