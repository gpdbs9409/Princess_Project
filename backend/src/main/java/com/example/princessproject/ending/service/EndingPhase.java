package com.example.princessproject.ending.service;

/** 엔딩 공개 일정상 현재 구간. */
public enum EndingPhase {
    /** 공개 전 (~ 9/30 23:59) - GNB 탭은 공개 전 페이지(UI-0X-01)로 간다. */
    BEFORE_REVEAL,
    /** 공개 중 (10/1 00:00 ~ 10/20 23:59) - 엔딩 페이지/공개 팝업 노출. */
    REVEALED,
    /** 운영 종료 (10/21 00:00 ~) - 관리자 외 로그인 및 API 접근 차단. */
    CLOSED
}
