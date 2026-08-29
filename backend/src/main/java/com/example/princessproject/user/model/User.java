package com.example.princessproject.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String nickname;

    @Column(length = 255)
    private String passwordHash;

    // 선택 항목 - 없으면 "비밀번호 찾기"를 쓸 수 없다. 회원가입 시 입력하거나 마이페이지에서 나중에 등록.
    @Column(length = 255, unique = true)
    private String email;

    @Column(length = 500)
    private String profileImageUrl;

    // 선택 입력. 참가자끼리 서로를 찾을 수 있게 프로필 목록에 노출된다. '@'는 저장하지 않고
    // 핸들만 담는다(표시할 때 붙임).
    @Column(length = 30)
    private String instagram;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role = Role.USER;

    // NULL = 아직 기수 배정 전(지원자). 값이 있으면 해당 기수의 실제 참가자로 취급한다.
    // 자유 텍스트("1기", "2기"...)로 두어 운영진이 새 기수를 코드 배포 없이 바로 쓸 수 있게 한다.
    @Column(length = 20)
    private String cohort;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    public User(String nickname, String passwordHash) {
        this.nickname = nickname;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
