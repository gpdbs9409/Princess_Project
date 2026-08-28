# 프린세스 다이어리 — 정보구조(IA) 및 서비스 설명서

- 서비스 URL: https://princess-project-2026.vercel.app
- 최종 갱신: 2026-08-08 (코드 기준)

---

## 1. 서비스 개요

30일 동안 매일 미션을 인증하며 스스로를 성장시키는 챌린지 서비스입니다.
소설 빙의물 세계관을 차용해, 참가자는 '시녀'에서 시작해 30일 뒤 '공주'로 엔딩을 바꿉니다.

| 항목 | 내용 |
| --- | --- |
| 운영 단위 | 기수제 (1기, 2기 … 자유 텍스트) |
| 기간 | 1기수 30일 |
| 참가비 | 20,000원 (반환 없음) |
| 예치금 | 100,000원 |
| 환급 | 주 6일 이상 인증 성공 시 25,000원 / 4주 완주 시 전액 |
| 성장 축 | 아비투스 7자본 중 3개 선택 + 공통과제 3종 |

**아비투스 7자본**: 심리 · 문화 · 지식 · 경제 · 신체 · 언어 · 상징
**공통과제 3종**: 독서 · 공부 · 주간 회고 (전원 필수)

---

## 2. 사용자 유형

| 유형 | 진입 경로 | 권한 |
| --- | --- | --- |
| 미가입 방문자 | 카톡·인스타 초대 링크 | 로그인/회원가입, 비밀번호 찾기 |
| 참가자 (USER) | 회원가입 | 본인 미션·기록·리포트 |
| 운영진 (ADMIN) | `users.role = 'ADMIN'` | 참가자 전체 + 환급·MVP·보정·지원서 |

가입 계정은 모두 실제 참가자로 간주합니다. 앱 주소가 선발 완료된 인원에게만 전달되기
때문에, 앱 안에 '승인 대기' 상태가 따로 없습니다.

---

## 3. 화면 구조 (Sitemap)

```
/
├── /login                     로그인 · 회원가입          [비로그인]
│    └── 온보딩 브릿지 팝업      초대 링크 유입자용 스토리텔링
├── /forgot-password           비밀번호 찾기              [무관]
├── /reset-password            비밀번호 재설정            [무관]
│
├── /stat-focus                나의 아비투스 · 미션 설정   [로그인]
├── /record                    오늘 기록                  [로그인]
├── /dashboard                 대시보드                   [로그인]
├── /weekly-retrospective      주간 회고                  [로그인]
├── /butler                    집사 채팅                  [로그인]
├── /my-page                   마이페이지                 [로그인]
│
└── /admin                     운영 관리자                [ADMIN]
     ├── 참가자 (기수별 주간 관리)
     ├── 기수 미배정 회원
     └── 지원서 (내부 기록용)
```

정의되지 않은 경로는 로그인 여부에 따라 `/dashboard` 또는 `/login`으로 보냅니다.

---

## 4. 화면별 기능

### 4-1. `/login` — 로그인 · 회원가입

- 닉네임 + 비밀번호 로그인
- 회원가입 시 이메일 인증, 약관 동의, 프로필 사진(선택)
- 비로그인 상태로 진입 시 온보딩 브릿지 팝업 노출

### 4-2. `/stat-focus` — 나의 아비투스 · 미션 설정

최초 1회만 설정 가능합니다. 환급 심사가 최초 설정한 미션 기준으로 이뤄지기 때문입니다.

- 이상적인 나의 모습 / 나의 외적 추구미 / 목표로 하는 행동양식 입력
- 7자본 중 3개 선택 후 비중(%) 배분, 합계 100%
- 자본별 행동양식 → 구체 미션 선택 (목록에 없으면 직접 추가)
- 공통과제 3종 안내 (독서는 중복 방지를 위해 선택 목록에서 제외)
- 설정 완료 후에는 읽기 전용 화면으로 전환

### 4-3. `/record` — 오늘 기록

- 미션별 인증 사진: **인앱 카메라 촬영** 또는 **갤러리에서 선택** (2026-08-21 정책 변경)
- 카메라 촬영 시 긴 변 1,024px로 축소 후 업로드
- AI Vision이 사진과 미션의 정합성 판독
- 공통과제(독서 페이지 수, 공부 진도) 입력

### 4-4. `/dashboard` — 대시보드

- 프로필 헤더 (게시물 수, 참가자 수)
- 자본별 진행률 · 스탯 미터
- 주간 막대 차트 / 자본별 추이 라인 차트
- D-day 위젯, 참가자 목록 모달

### 4-5. `/weekly-retrospective` — 주간 회고

공통과제 중 하나로, 3부 구성입니다.

1. PART1 일상 공유
2. PART2 이번 주 회고
3. PART3 다음 주 계획

### 4-6. `/butler` — 집사 채팅

- '레오집사'의 한마디를 날짜별로 이어서 조회 (말풍선 UI)
- **'오늘의 한마디 듣기' 버튼으로 당일 AI 피드백 생성** — 오늘 기록을 저장한 뒤 사용 가능
- 피드백에는 서버가 이미 계산한 점수·미션명만 전달 (원본 기록·닉네임 미전송)

### 4-7. `/my-page` — 마이페이지

프로필 사진 변경, 이메일 등록, 누적 기록 수 확인.

### 4-8. `/admin` — 운영 관리자

| 탭 | 기능 |
| --- | --- |
| 참가자 (기수별 주간 관리) | 주간 성공일수, 환급 대상 판정, 지급 체크, MVP 지정, 점수 보정, CSV 내보내기 |
| 기수 미배정 회원 | 가입했으나 기수 태그가 없는 회원에게 기수 배정 |
| 지원서 (내부 기록용) | 모집 단계 지원자 수기 입력 + CSV 일괄 등록 |

참가자·미배정 회원 목록에서 운영진 계정에는 `관리자` 태그가 표시됩니다.

---

## 5. API 구조

인증은 JWT Bearer 토큰, 세션은 STATELESS입니다.

### 5-1. 인증 (공개)

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인, JWT 발급 |
| POST | `/api/auth/email-verification/request` | 이메일 인증 코드 발송 |
| POST | `/api/auth/email-verification/confirm` | 인증 코드 확인 |
| POST | `/api/auth/forgot-password` | 비밀번호 재설정 링크 발송 |
| POST | `/api/auth/reset-password` | 비밀번호 재설정 |

### 5-2. 회원 (본인만)

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/users/{id}` | 회원 정보 조회 |
| PUT | `/api/users/{id}/profile-image` | 프로필 사진 변경 |
| PUT | `/api/users/{id}/email` | 이메일 등록·변경 |
| GET | `/api/users/{id}/profile-stats` | 누적 기록 수, 전체 참가자 수 |
| GET | `/api/users/{id}/participants` | 함께하는 참가자 목록 |

### 5-3. 카탈로그 · 프로젝트 (로그인)

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/catalog` | 7자본 · 행동양식 · 미션 기준 정보 |
| GET | `/api/projects/active` | 진행 중인 프로젝트 조회 |
| PUT | `/api/projects/active/selections` | 아비투스·미션 일괄 저장 (최초 1회) |

### 5-4. 일일 기록 · 리포트 (로그인)

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/records` | 미션 인증 기록 저장 |
| GET | `/api/projects/active/daily` | 당일 미션 진행률 |
| GET | `/api/projects/active/weekly-report` | 주간 리포트 (7일치) |

### 5-5. 공통과제 (로그인)

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/common-tasks` | 공통과제 기록 저장 |
| GET | `/api/common-tasks/daily` | 일일 공통과제(독서·공부) 조회 |
| GET | `/api/common-tasks/weekly` | 주간 회고 조회 |
| GET | `/api/common-tasks/weekly/history` | 주간 회고 이력 |
| PUT | `/api/common-tasks/weekly/{recordId}` | 주간 회고 수정 |

### 5-6. AI (로그인)

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/vision/analyze` | 인증 사진 판독 |
| POST | `/api/projects/active/ai-feedback` | 집사의 한마디 생성 |
| GET | `/api/projects/active/ai-feedback/history` | 집사 피드백 이력 |

### 5-7. 업로드

| 메서드 | 경로 | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/uploads` | 로그인 | 파일 업로드, URL 반환 |
| GET | `/api/uploads/{key}` | 공개 | 파일 조회 (키는 UUID) |

### 5-8. 관리자 (ADMIN)

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| PUT | `/api/admin/users/{userId}/role` | 운영진 승격·강등 |
| GET | `/api/admin/cohorts` | 기수 목록 |
| GET | `/api/admin/applicants` | 기수 미배정 회원 목록 |
| PUT | `/api/admin/members/{userId}/cohort` | 기수 배정 |
| GET | `/api/admin/participants` | 기수별 주간 현황 (성공일수·환급·MVP) |
| GET | `/api/admin/members/{userId}/activities` | 개인 활동 상세 |
| PUT | `/api/admin/members/{userId}/refund` | 환급 지급 여부 체크 |
| PUT | `/api/admin/mvp` | 주간 MVP 지정 |
| DELETE | `/api/admin/mvp` | 주간 MVP 해제 |
| POST | `/api/admin/members/{userId}/adjustments` | 점수 보정 추가 |
| GET | `/api/admin/members/{userId}/adjustments` | 점수 보정 이력 |
| DELETE | `/api/admin/adjustments/{adjustmentId}` | 점수 보정 롤백 |
| GET | `/api/admin/recruitment-applicants` | 지원서 목록 |
| POST | `/api/admin/recruitment-applicants` | 지원서 수기 등록 |
| POST | `/api/admin/recruitment-applicants/bulk` | 지원서 CSV 일괄 등록 |
| PUT | `/api/admin/recruitment-applicants/{id}` | 지원서 수정 |
| DELETE | `/api/admin/recruitment-applicants/{id}` | 지원서 삭제 |

---

## 6. 데이터 모델

### 회원 · 프로젝트

```
users
 └─ user_projects            (1기수 = 1프로젝트)
     └─ user_goals           (선택한 7자본 중 3개 + 비중)
         └─ user_stats       (자본 내 행동양식)
             └─ user_missions (구체 미션)
```

### 기록

| 테이블 | 내용 |
| --- | --- |
| `daily_records` | 미션별 일일 인증 기록 (사진 URL, 수치) |
| `common_task_records` | 공통과제 3종 기록 |
| `ai_feedbacks` | 집사 피드백 이력 |

### 운영

| 테이블 | 내용 |
| --- | --- |
| `weekly_refunds` | 주차별 환급 지급 여부·지급일 |
| `weekly_mvp` | 기수·주차별 MVP |
| `score_adjustments` | 점수 보정 이력 (삭제로 롤백) |
| `recruitment_applicants` | 모집 지원서 (users와 무관) |

### 기준 정보

`goal_types`(7자본) · `stat_types`(행동양식) · `mission_definitions`(미션 카탈로그)

### 인증 보조

`password_reset_tokens` · `email_verifications`

---

## 7. 핵심 규칙

**인증 사진**

초기에는 조작 방지를 위해 인앱 카메라 촬영만 허용했습니다. 다만 독서·공부처럼 화면
캡처가 자연스러운 미션이 있어, 2026-08-21에 갤러리 업로드를 다시 허용했습니다.
현재는 두 경로 모두 지원하며, 카메라 촬영분은 canvas로 인코딩되어 EXIF가 남지 않습니다.
백엔드의 촬영일시 검증은 EXIF가 존재하면서 날짜가 어긋난 경우에만 반려하도록
느슨하게 유지하고 있습니다.

**성공일수 판정**

하루 단위로 그날 활성화된 미션 기준입니다.

| 상태 | 점수 |
| --- | --- |
| 전부 완료 | 1.0일 |
| 일부 완료 | 0.5일 |
| 미완료 | 0일 |

주간 합계 6일 이상이면 환급 대상으로 자동 표시되며, 실제 지급 여부는 운영진이 확인 후
직접 체크합니다.

**미션 수정 불가**

최초 설정 후 변경할 수 없습니다. 부적절한 미션은 운영진이 개별 안내합니다.

---

## 8. 기술 구성

| 영역 | 스택 | 호스팅 |
| --- | --- | --- |
| 프론트엔드 | React 19 · TypeScript · Vite · React Router 7 | Vercel |
| 백엔드 | Spring Boot · Spring Security · JPA | Railway (US West) |
| DB | MySQL | Railway |
| 파일 저장 | S3 호환 버킷 | Railway Buckets |
| AI | OpenAI Chat Completions | — |

**AI 모델 분리**

| 용도 | 모델 | 사유 |
| --- | --- | --- |
| 사진 판독 | `gpt-4.1-mini` | 이미지 토큰 계산 방식이 유리 |
| 텍스트 피드백 | `gpt-4o-mini` | 텍스트 단가가 저렴 |

**운영비**: 월 약 33,700원 (Railway Pro $20 = 28,400원 + AI 5,300원 + Vercel 0원)
※ SMTP(이메일 발송)를 위해 Railway Pro로 상향. 기존 dev+prod $18 대비 증분 $2

---

## 9. 배포 환경 (dev / prod)

Railway 한 프로젝트 안에 **prod·dev 두 개의 Environment**를 두고, 동일한 템플릿으로
구성했습니다. 서버·MySQL·버킷이 환경마다 각각 존재해서 **DB가 완전히 분리**됩니다.
테스트 데이터가 운영 데이터에 섞이지 않고, 스키마 마이그레이션도 dev에서 먼저 검증한
뒤 prod에 적용할 수 있습니다.

| 구분 | prod | dev |
| --- | --- | --- |
| 연결 브랜치 | `develop` | `dev` |
| 백엔드 | Railway (Environment: prod) | Railway (Environment: dev) |
| DB | 전용 MySQL 인스턴스 | 전용 MySQL 인스턴스 |
| 버킷 | 전용 Railway Bucket | 전용 Railway Bucket |
| 프론트엔드 | Vercel Production | 로컬(`localhost:5173`) 또는 Preview |

### 배포 흐름

```
로컬 개발 (localhost:5173 → localhost:8080)
        ↓ push
   dev 브랜치 → Railway dev 환경 자동 배포
        ↓ 검증 후 merge
develop 브랜치 → Railway prod 환경 + Vercel Production 자동 배포
```

### 환경 분리 방식

같은 이미지를 쓰고 **환경변수만 다르게** 주입합니다. 코드에는 환경 분기가 없습니다.

| 환경변수 | 용도 |
| --- | --- |
| `MYSQLHOST` 등 | Railway MySQL 플러그인 참조 (`${{MySQL.MYSQLHOST}}`) |
| `BUCKET_*` | 버킷 엔드포인트·자격증명 |
| `OPENAI_API_KEY` | OpenAI 인증 키 |
| `OPENAI_MODEL_VISION` / `_FEEDBACK` | 모델 지정 (기본값은 코드에 존재) |
| `JWT_SECRET` / `JWT_EXPIRATION_MINUTES` | 토큰 서명 키·만료 |
| `CORS_ALLOWED_ORIGINS` | 허용 오리진 (콤마 구분) |

`OPENAI_API_KEY`가 비어 있으면 `MockVisionClient` · `MockAiFeedbackClient`가 대신
주입되도록 `@ConditionalOnExpression`을 걸어두었습니다. 키 없이도 로컬에서 전체
플로우를 돌릴 수 있고, 개발 중 불필요한 API 과금이 발생하지 않습니다.

파일 저장도 같은 방식으로, 버킷 설정이 없으면 `LocalFileStorageClient`가 로컬
디렉터리에 저장합니다.

---

## 10. 보안 · 인프라

### 인증 · 인가

| 항목 | 구현 |
| --- | --- |
| 비밀번호 | BCrypt 해시 저장 (평문 미보관) |
| 토큰 | JWT HS256, 서버 세션 없음(STATELESS) |
| 토큰 클레임 | `userId`, `nickname`, `role` |
| 인가 | `/api/admin/**`은 `hasRole("ADMIN")`, 그 외 `authenticated()` |
| 본인 확인 | `@PreAuthorize("#id == authentication.principal")` (6곳) |
| 만료 처리 | 401 응답 시 프론트에서 전역 로그아웃 |

관리자 권한은 `users.role` 컬럼이 단일 진실 공급원입니다. 로그인 시 권한을 재계산하지
않으므로, 설정 파일이나 환경변수 변경으로 권한이 뒤바뀌지 않습니다.

### 비밀번호 재설정 · 이메일 인증

| 항목 | 구현 |
| --- | --- |
| 토큰 생성 | `SecureRandom` + Base64 URL-safe 인코딩 |
| 유효기간 | 발급 후 제한 시간 경과 시 만료 |
| 재사용 방지 | 1회 사용 후 `used` 플래그 처리 |
| 실패 구분 | `TOKEN_INVALID` / `TOKEN_EXPIRED` / `TOKEN_ALREADY_USED` |

### 데이터 보호

- **DB 스키마**: `ddl-auto=validate`. 애플리케이션이 운영 스키마를 자동 변경하지 않고,
  마이그레이션 SQL을 사람이 검토·적용합니다.
- **참조 무결성**: 사용자 하위 데이터는 `ON DELETE CASCADE`, 기준 정보(자본·미션 카탈로그)는
  `ON DELETE RESTRICT`로 실수로 인한 삭제를 차단합니다.
- **AI 전송 최소화**: 피드백 생성 시 원본 기록·닉네임을 보내지 않고, 서버가 계산한
  점수·미션명만 전달합니다.
- **국외 이전 고지**: OpenAI(미국)·Railway(미국)·Vercel(미국)을 약관에 명시했습니다.

### 인프라

| 항목 | 구성 |
| --- | --- |
| 컨테이너 | Dockerfile 멀티스테이지 (`temurin:21-jdk` 빌드 → `21-jre` 실행) |
| CORS | 허용 오리진 화이트리스트, 환경변수로 관리 |
| 업로드 | 최대 10MB, 키는 `UUID.randomUUID()` (122비트 랜덤) |
| 정적 파일 | S3 호환 버킷, 애플리케이션 경유 서빙 |

### 알려진 한계

문서화 목적상 현재 구조의 약점도 함께 적어둡니다.

| 항목 | 현황 | 개선 방향 |
| --- | --- | --- |
| JWT 만료 30일 | 리프레시 토큰이 없어 만료 기간으로 대체. 로그아웃해도 토큰 자체는 유효 | 짧은 액세스 토큰 + 리프레시 토큰 도입 |
| 업로드 파일 조회 | `GET /api/uploads/{key}`가 인증 없이 열려 있음 (키 추측은 어려움) | 서명된 URL 또는 소유자 검증 |
| 마이그레이션 이력 | 적용 여부가 코드로 남지 않음 | Flyway 도입 |

---

## 11. 미구현 (다음 단계)

- 엔딩 리포트 · 자본별 엔딩 캐릭터 배정
- MVP 보너스 · 점수 보정의 대시보드 점수 자동 반영
- 완주 환급(4주 전액) 자동 계산
- 초대장 랜딩 페이지
