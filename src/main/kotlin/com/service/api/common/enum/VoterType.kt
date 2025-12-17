package com.service.api.common.enum

enum class VoterType {
    ELIGIBLE, // 투표 가능

    INCOMPLETE, // 투표 불가(추가 정보 미입력)
    FOREIGNER, // 투표 불가(외국인)
    UNDERAGE, // 투표 불가(미성년자)
    BLOCKED, //  투표 불가(운영 임의로 일시중단)
    ;
}
