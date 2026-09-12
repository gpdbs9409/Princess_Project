package com.example.princessproject.common;

import java.util.Arrays;
import java.util.List;

/**
 * 인증 사진 여러 장 지원 (2026-09 요청: "인증사진 여러개 들어갈수있었으면 좋겠다"). 대표 사진
 * (photoUrl)만 기존처럼 AI 비전 판정 대상이고, 여기서 다루는 "추가 사진"들은 참고용 추가 증빙이라
 * 별도 자식 테이블 없이 한 컬럼에 쉼표로 구분해 저장한다 - 앱 규모상 이 정도로 충분하고, 매
 * 엔티티(DailyRecord, CommonTaskRecord)에 자식 테이블을 새로 만드는 것보다 훨씬 단순하다.
 */
public final class PhotoUrlListCodec {

    /** 너무 많은 사진을 한 컬럼에 우겨넣지 않도록 상한을 둔다. */
    private static final int MAX_URLS = 4;

    private PhotoUrlListCodec() {
    }

    public static String encode(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        String joined = urls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .limit(MAX_URLS)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
        return (joined == null || joined.isBlank()) ? null : joined;
    }

    public static List<String> decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
