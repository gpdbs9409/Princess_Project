# Princess Project API 명세서

- Base URL (개발): `http://localhost:8080`
- 데이터 포맷: JSON (업로드/비전 분석 엔드포인트만 `multipart/form-data`)
- 인증: `Authorization: Bearer <JWT>` 헤더. 쿠키/세션 사용 안 함 (stateless).
- CORS: `cors.allowed-origins` (기본 `http://localhost:5173`, `CORS_ALLOWED_ORIGINS` 환경변수로 오버라이드) origin만 허용.

## 인증 모델

| 항목 | 내용 |
|---|---|
| 로그인 방식 | 닉네임만 입력 (비밀번호 없음) — 최초 로그인 시 사용자 자동 생성 |
| 토큰 | JWT (HS256), subject = `userId` |
| 만료시간 | `jwt.expiration-minutes` (기본 1440분 = 24시간) |
| 인증 실패 응답 | `401 Unauthorized` (토큰 없음/만료/위조) |
| 인가 실패 응답 | `403 Forbidden` (다른 사용자 리소스 접근 시 `@PreAuthorize` 거부) |
| 로그아웃/재발급 | 없음 (클라이언트가 로컬 저장소 토큰 폐기) |
| 알려진 제약 | 닉네임이 유일한 인증 수단 — 타인 닉네임 선점 시 그 계정으로 로그인 가능. MVP 단계 의도된 단순화, 정식 배포 전 비밀번호/OAuth 보강 필요 |

## 에러 응답 코드 (실제 구현 기준)

| 상황 | 상태 코드 | 비고 |
|---|---|---|
| 요청 바디 검증 실패 (`@Valid`) | 400 | Spring 기본 처리 |
| 토큰 없음/만료/위조 | 401 | |
| 다른 사용자 리소스 접근 | 403 | `@PreAuthorize` 거부 |
| 존재하지 않는 userId/missionId 조회 | 500 | 전역 예외 핸들러 없음 — `IllegalArgumentException`이 404가 아닌 500으로 응답됨 (개선 여지 있음) |
| 빈 파일 업로드 | 500 | 위와 동일 사유 |

## 공통 코드값

**StatType** (7종, 대문자 문자열로 직렬화)

| 코드 | 의미 |
|---|---|
| PHYSICAL | 신체 |
| ECONOMY | 경제 |
| CULTURE | 문화 |
| KNOWLEDGE | 지식 |
| LANGUAGE | 언어 |
| PSYCHOLOGY | 심리 |
| SYMBOL | 상징 |

**MissionType**

| 코드 | 의미 |
|---|---|
| DAILY | 일일 미션 (현재 시딩된 미션은 전부 이 타입) |
| WEEKLY | 주간 미션 (미사용) |
| TOTAL | 누적 미션 (미사용) |

---

## 엔드포인트 목록

| # | Method | Path | 인증 | 설명 |
|---|---|---|---|---|
| 1 | POST | `/api/auth/login` | 공개 | 닉네임 로그인/가입 |
| 2 | GET | `/api/users/{id}` | 본인 | 사용자 조회 |
| 3 | PUT | `/api/users/{id}/stat-focus` | 본인 | 스탯 비중 설정 (전체 교체) |
| 4 | GET | `/api/missions` | 로그인 | 미션 카탈로그 조회 |
| 5 | POST | `/api/records` | 본인 | 행동 기록 입력(upsert) + 점수 재계산 |
| 6 | GET | `/api/users/{userId}/daily` | 본인 | 특정일 요약 조회 |
| 7 | POST | `/api/users/{userId}/ai-feedback` | 본인 | AI 피드백 생성 |
| 8 | GET | `/api/users/{userId}/weekly-report` | 본인 | 주간 리포트 조회 |
| 9 | POST | `/api/uploads` | 로그인 | 파일 업로드 |
| 10 | POST | `/api/vision/analyze` | 로그인 | 인증 사진 진위 분석 (더미 로직) |

---

## 1. `POST /api/auth/login`

닉네임으로 로그인. 존재하지 않는 닉네임이면 자동 회원가입.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| nickname | string | O | 공백 불가 |

**Response 200 Body**

| 필드 | 타입 | 설명 |
|---|---|---|
| token | string | JWT |
| user | object | 아래 UserResponse 참고 |
| user.id | long | 사용자 ID |
| user.nickname | string | 닉네임 |
| user.goalHuman | string \| null | 목표(이상적 자아상) |
| user.goalEnding | string \| null | 행동양식/목표 설명 |
| user.statFocus | map<StatType, int> | 스탯별 비중(%) |

---

## 2. `GET /api/users/{id}`

🔒 본인만 (`#id == principal`)

**Path Params**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| id | long | O | 사용자 ID |

**Response 200 Body** — UserResponse (위 1번과 동일 구조)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | long | 사용자 ID |
| nickname | string | 닉네임 |
| goalHuman | string \| null | 목표 |
| goalEnding | string \| null | 행동양식 |
| statFocus | map<StatType, int> | 스탯별 비중(%) |

---

## 3. `PUT /api/users/{id}/stat-focus`

🔒 본인만. 기존 비중 목록을 전체 삭제 후 재저장(overwrite).

**Path Params**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| id | long | O | 사용자 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| stats | array\<StatFocusItem\> | O | 최소 1개 이상 |
| stats[].statType | StatType | O | 스탯 코드 |
| stats[].weightPercent | int | O | 0~100 비중 |

**Response 200 Body** — UserResponse (2번과 동일)

---

## 4. `GET /api/missions`

🔒 로그인 필요 (사용자 소유권 없는 공용 카탈로그). id 오름차순 정렬 배열 반환.

**Response 200 Body** — MissionResponse 배열

| 필드 | 타입 | 설명 |
|---|---|---|
| id | long | 미션 ID |
| name | string | 미션명 (예: 독서) |
| missionType | MissionType | 미션 주기 |
| statType | StatType | 연결된 스탯 |
| assignedPoints | int | 완료 시 배정 점수 |
| targetValue | double | 목표량 (예: 15) |
| unit | string | 단위 (예: 분) |
| common | boolean | true면 사용자 스탯 비중과 무관하게 항상 정식 점수 반영, false면 focus에 포함된 스탯일 때만 정식 점수(그 외는 bonusScore로만 집계) |

---

## 5. `POST /api/records`

🔒 본인만 (`request.userId == principal`). 같은 user+mission+date 조합이면 upsert, 저장 후 해당 날짜 점수 재계산.

**Request Body** — RecordRequest

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| userId | long | O | 사용자 ID |
| missionId | long | O | 미션 ID |
| date | date (yyyy-MM-dd) | O | 기록 날짜 |
| inputValue | double | O | 행동값 (예: 독서 15분 → 15) |
| photoUrl | string | X | 업로드 API로 받은 URL |
| memo | string | X | 메모 |

**Response 200 Body** — DailySummaryResponse

| 필드 | 타입 | 설명 |
|---|---|---|
| date | date | 대상 날짜 |
| totalScore | double | 그날 총점 |
| progress | double | 진행률 (0~1) |
| statScores | map<string, double> | 스탯별 점수 (키는 소문자 statType) |
| completedMissions | array\<string\> | 완료한 미션명 목록 |
| remainingMissions | array\<string\> | 미완료 미션명 목록 |
| aiFeedback | object \| null | 그날 이미 생성된 AI 피드백 (없으면 null) |

---

## 6. `GET /api/users/{userId}/daily`

🔒 본인만. 새 기록 생성 없이 현재 상태만 조회.

**Path/Query Params**

| 필드 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| userId | path | long | O | 사용자 ID |
| date | query | date (yyyy-MM-dd) | O | 조회 날짜 |

**Response 200 Body** — DailySummaryResponse (5번과 동일 구조)

---

## 7. `POST /api/users/{userId}/ai-feedback`

🔒 본인만. 그날 누적 점수/완료 미션을 바탕으로 AI 피드백을 새로 생성(기존 피드백 덮어씀). AI에는 원본 입력값이 아닌 집계 수치만 전달.

**Path/Query Params**

| 필드 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| userId | path | long | O | 사용자 ID |
| date | query | date (yyyy-MM-dd) | O | 대상 날짜 |

**Response 200 Body** — AiFeedbackResponse

| 필드 | 타입 | 설명 |
|---|---|---|
| summary | string | 전체 요약 |
| praise | string | 칭찬 |
| improvement | string | 개선점 |
| tomorrow | string | 내일 제안 |
| cheer | string | 응원 문구 |

---

## 8. `GET /api/users/{userId}/weekly-report`

🔒 본인만. `weekStart` 포함 7일(`weekStart` ~ `weekStart+6`) 집계.

**Path/Query Params**

| 필드 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| userId | path | long | O | 사용자 ID |
| weekStart | query | date (yyyy-MM-dd) | O | 주 시작일 (해당일 포함 7일 집계) |

**Response 200 Body** — WeeklyReportResponse

| 필드 | 타입 | 설명 |
|---|---|---|
| weekStart | date | 주 시작일 |
| weekEnd | date | 주 종료일 (weekStart+6) |
| totalScore | double | 7일 총점 합계 |
| averageProgress | double | 평균 진행률 (기록 없는 날도 0으로 계산, 항상 7로 나눔) |
| statScoreTotals | map<string, double> | 스탯별 7일 합계 |
| missionCompletionCounts | map<string, int> | 미션별 완료 횟수 |
| dailyBreakdown | array\<DailySummaryResponse\> | 일자별 상세 (각 항목의 aiFeedback은 항상 null) |

---

## 9. `POST /api/uploads`

🔒 로그인 필요. `multipart/form-data`. 최대 10MB.

**Request (multipart fields)**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| file | file | O | 업로드할 파일 |

**Response 200 Body**

| 필드 | 타입 | 설명 |
|---|---|---|
| url | string | 저장된 파일 URL (`aws.s3.bucket` 미설정 시 로컬 `/uploads/...`, 설정 시 S3 URL) |

---

## 10. `POST /api/vision/analyze`

🔒 로그인 필요. ⚠️ 현재 파일명 키워드(`book`/`read`/`study`/`learning`) 기반 더미 로직이며 실제 이미지 분석 아님 — 임시 스텁으로 취급. `multipart/form-data`.

**Request (multipart fields)**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| file | file | O | 인증 사진 |
| expectedTopic | string | X (기본값 "독서") | 인증 대상 행동/미션명 |

**Response 200 Body**

| 필드 | 타입 | 설명 |
|---|---|---|
| likelyValid | boolean | 사진이 해당 주제와 관련 있어 보이는지 |
| reason | string | 판단 사유 텍스트 |
| confidence | string | 신뢰도 ("low" \| "medium") |
