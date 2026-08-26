package com.example.princessproject.user.dto;

import com.example.princessproject.user.model.User;

/**
 * 대시보드 팔로워/팔로잉 클릭 시 보여주는 "같은 기수 다른 참가자" 목록용 - 실제 팔로우 관계 데이터는
 * 없어서(그런 테이블/기능 자체가 없음), 같은 기수(cohort) 참가자를 그냥 나열해서 보여준다
 * (2026-08 요청: "다른 참가자들 간단하게 프로필 리스트 볼 수 있도록"). 그래서 email/role 같은
 * 민감한 정보는 빼고 닉네임/프로필사진만 내려준다.
 */
public record ParticipantResponse(Long id, String nickname, String profileImageUrl) {
    public static ParticipantResponse from(User user) {
        return new ParticipantResponse(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }
}
