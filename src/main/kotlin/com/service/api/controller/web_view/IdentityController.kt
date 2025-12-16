package com.service.api.controller.web_view

import com.service.api.service.IdentityService
import com.service.api.util.UrlUtil
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/web-view")
class IdentityController(
    private val identityService: IdentityService,

    @Value("\${identity.kcb.popup-url}") private val kcbPopupUrl: String,
    @Value("\${identity.kcb.cp-id}") private val kcbCpId: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/identity")
    fun getIdentityPage(): ResponseEntity<Void> {
        return ResponseEntity
            .status(HttpStatus.FOUND) // 302
            .header(HttpHeaders.LOCATION,
                if (identityService.isKcbLicenseLoaded()) "/web-view/identity-prod" else "/web-view/identity-mock")
            .build()
    }

    @GetMapping("/identity-prod")
    fun getIdentityProdPage(request: HttpServletRequest): ResponseEntity<String> {
        val returnUrl = "${UrlUtil.getCurrentBaseUrl(request)}/web-view/identity/callback"
        val mdlTkn = identityService.requestKcbPhoneIdentityVerification(returnUrl)

        val html = """
            <html>
            <title>KCB 휴대폰 본인확인 서비스</title>
            <head>
            <meta http-equiv="Content-Type" content="text/html; charset=euc-kr">
            <script type="text/javascript">
            	function request(){
            		document.form1.action = "$kcbPopupUrl";
            		document.form1.method = "post";

            		document.form1.submit();
            	}
            </script>
            </head>

            <body>
            	<form name="form1">
                <!-- 인증 요청 정보 -->
	            <!--// 필수 항목 -->
            	<input type="hidden" name="tc" value="kcb.oknm.online.safehscert.popup.cmd.P931_CertChoiceCmd"/>
            	<input type="hidden" name="cp_cd" value="$kcbCpId">
            	<input type="hidden" name="mdl_tkn" value="$mdlTkn">
            	<input type="hidden" name="target_id" value="">
                <!-- 필수 항목 //-->
            	</form>
            </body>
            <script>request();</script>
            </html>
        """.trimIndent()

        return ResponseEntity.ok()
            .header("Content-Type", "text/html; charset=UTF-8")
            .body(html)
    }

    @GetMapping("/identity-mock")
    fun getIdentityMockPage(): ResponseEntity<String> {
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

                <form id="f" action="/web-view/identity/callback" method="get">
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

                  <input type="hidden" name="mdl_tkn" id="token">

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
                    document.getElementById('token').value = "_" + b64(nameVal + "@" + f + "-" + b);
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

    @GetMapping("/identity/callback")
    fun getIdentityCallbackPage(
        @RequestParam(value = "mdl_tkn", required = false, defaultValue = "") mdlTkn: String,
    ): ResponseEntity<Void> {
        log.info("identity callback page mdl_tkn: {}", mdlTkn)
        return ResponseEntity
            .status(HttpStatus.FOUND) // 302
            .header(HttpHeaders.LOCATION, "/web-view/identity/exit?MDL_TKN=$mdlTkn&isSuccess=${mdlTkn.isNotBlank()}")
            .build()
    }

    @GetMapping("/identity/exit")
    fun getIdentityExitPage(@RequestParam params: MultiValueMap<String, String>): ResponseEntity<Void> {
        log.info("identity exit page query param: {}", params)
        return ResponseEntity.ok().build()
    }
}
