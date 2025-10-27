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
        val targetUrl = "/web-view/identity/exit"
        val html = """
            <html lang="ko">
              <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>본인인증(Mock)</title>
              </head>
              <body style="text-align:center; margin-top:20%; font-family:sans-serif;">
                <h2>본인인증(Mock)</h2>
                <h6>(참고사항)<br>- 입력한 정보가 전부 동일하면 CI값도 동일하게 생성됨<br>- 주민번호 앞자리로 만 나이 판단 (14세 미만 & 유권자)<br>- 뒷1자리로 내외국인 & 성별 판단</h6>

                <form id="f" action="$targetUrl" method="GET">
                  <div>
                    <label>이름</label><br>
                    <input id="userName" type="text" style="width:80px; text-align:center;"required>
                  </div><br>

                  <div>
                    <label>주민번호</label><br>
                    <input id="front" type="text" placeholder="YYMMDD" maxlength="6" style="width:90px; text-align:center;" required>
                    <span>-</span>
                    <input id="back" type="text" maxlength="1" style="width:20px;" required>
                    <span>XXXXXX</span>
                  </div><br>

                  <input type="hidden" name="MDL_TKN" id="token">
                  <input type="hidden" name="isSuccess" value="true">

                  <button type="submit">인증 완료하기</button>
                </form>

                <script>
                  function validDate(s) {
                    if (!/^\d{6}${'$'}/.test(s)) return false;
                    const y = +s.slice(0,2), m = +s.slice(2,4)-1, d = +s.slice(4,6);
                    const year = (y <= new Date().getFullYear()%100 ? 2000+y : 1900+y);
                    const dt = new Date(year, m, d);
                    return dt.getFullYear()===year && dt.getMonth()===m && dt.getDate()===d;
                  }
                  function b64(s){
                    return btoa(unescape(encodeURIComponent(s))).replace(/\+/g,"-").replace(/\//g,"_").replace(/=+${'$'}/,"");
                  }
                  document.getElementById('f').onsubmit = function(e){
                    e.preventDefault();
                    const nameVal = document.getElementById('userName').value.trim();
                    const f = document.getElementById('front').value.trim();
                    const b = document.getElementById('back').value.trim();
                    if (!nameVal || !validDate(f) || !/^\d${'$'}/.test(b)) { alert("입력값을 확인하세요."); return; }
                    document.getElementById('token').value = b64(nameVal + "@" + f + "-" + b);
                    this.submit();
                  }
                </script>
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
