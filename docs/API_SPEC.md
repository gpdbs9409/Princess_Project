# Princess Project API 명세서

## **POST `/api/auth/login`**

닉네임으로 로그인한다. 존재하지 않는 닉네임이면 자동으로 회원가입된다.

- 인증: 불필요
- 접근 권한: 공개

### **Request Body**

```json
{ "nickname": "공주님" }
```

### **Request 필드**

- `nickname`: 닉네임 (공백 불가)

### **Response 200 OK**

```json
{
  "token": "eyJhbGciOi...",
  "user": {
    "id": 1,
    "nickname": "공주님",
    "goalHuman": null,
    "goalEnding": null,
    "statFocus": {}
  }
}
```

### **응답 필드**

- `token`: JWT
- `user.id`: 사용자 ID
- `user.nickname`: 닉네임
- `user.goalHuman`: 목표 인간상
- `user.goalEnding`: 목표 행동양식 또는 엔딩
- `user.statFocus`: `StatType`별 집중 비중

---

## **GET `/api/users/{id}`**

사용자 정보를 조회한다.

- 인증: 필요
- 접근 권한: 본인만 가능
- 권한 조건: `#id == principal`

### **Response 200 OK**

```json
{
  "id": 1,
  "nickname": "공주님",
  "goalHuman": "이상적인 나의 모습",
  "goalEnding": "목표로 하는 행동양식",
  "statFocus": {
    "PHYSICAL": 30,
    "KNOWLEDGE": 70
  }
}
```

### **응답 필드**

- `id`: 사용자 ID
- `nickname`: 닉네임
- `goalHuman`: 목표 인간상
- `goalEnding`: 목표 행동양식 또는 엔딩
- `statFocus`: `StatType`별 집중 비중

---

## **PUT `/api/users/{id}/stat-focus`**

사용자의 스탯 비중을 설정한다. 기존 비중을 전부 삭제하고 새로 저장한다(전체 교체).

- 인증: 필요
- 접근 권한: 본인만 가능
- 권한 조건: `#id == principal`

### **Request Body**

```json
{
  "stats": [
    { "statType": "PHYSICAL", "weightPercent": 30 },
    { "statType": "KNOWLEDGE", "weightPercent": 70 }
  ]
}
```

### **Request 필드**

- `stats`: 비중 목록 (최소 1개 이상)
- `stats[].statType`: 스탯 코드
- `stats[].weightPercent`: 비중 (0~100)

### **Response 200 OK**

```json
{
  "id": 1,
  "nickname": "공주님",
  "goalHuman": null,
  "goalEnding": null,
  "statFocus": {
    "PHYSICAL": 30,
    "KNOWLEDGE": 70
  }
}
```

### **응답 필드**

- `id`: 사용자 ID
- `nickname`: 닉네임
- `goalHuman`: 목표 인간상
- `goalEnding`: 목표 행동양식 또는 엔딩
- `statFocus`: `StatType`별 집중 비중 (갱신된 값)

---

## **GET `/api/missions`**

미션 카탈로그를 조회한다.

- 인증: 필요
- 접근 권한: 로그인한 사용자 누구나 (사용자 소유권 없는 공용 데이터)

### **Response 200 OK**

```json
[
  {
    "id": 1,
    "name": "독서",
    "missionType": "DAILY",
    "statType": "KNOWLEDGE",
    "assignedPoints": 20,
    "targetValue": 15.0,
    "unit": "분",
    "common": true
  }
]
```

### **응답 필드**

- `id`: 미션 ID
- `name`: 미션명
- `missionType`: 미션 주기 (`DAILY`/`WEEKLY`/`TOTAL`)
- `statType`: 연결된 스탯
- `assignedPoints`: 완료 시 배정 점수
- `targetValue`: 목표량
- `unit`: 단위
- `common`: true면 스탯 비중과 무관하게 항상 정식 점수 반영, false면 focus에 포함된 스탯일 때만 정식 점수

---

## **POST `/api/records`**

행동 기록을 입력한다(upsert). 저장 후 해당 날짜의 점수를 재계산한다.

- 인증: 필요
- 접근 권한: 본인만 가능
- 권한 조건: `#request.userId() == principal`

### **Request Body**

```json
{
  "userId": 1,
  "missionId": 3,
  "date": "2026-07-10",
  "inputValue": 15.0,
  "photoUrl": "https://.../uploads/xxx.jpg",
  "memo": "오늘은 15분 읽었다"
}
```

### **Request 필드**

- `userId`: 사용자 ID
- `missionId`: 미션 ID
- `date`: 기록 날짜
- `inputValue`: 행동값 (예: 독서 15분 → 15)
- `photoUrl`: 업로드 API로 받은 URL (선택)
- `memo`: 메모 (선택)

### **Response 200 OK**

```json
{
  "date": "2026-07-10",
  "totalScore": 45.0,
  "progress": 0.75,
  "statScores": { "physical": 10.0, "knowledge": 20.0 },
  "completedMissions": ["독서"],
  "remainingMissions": ["운동", "가계부 작성"],
  "aiFeedback": null
}
```

### **응답 필드**

- `date`: 대상 날짜
- `totalScore`: 그날 총점
- `progress`: 진행률 (0~1)
- `statScores`: 스탯별 점수 (키는 소문자 statType)
- `completedMissions`: 완료한 미션명 목록
- `remainingMissions`: 미완료 미션명 목록
- `aiFeedback`: 그날 이미 생성된 AI 피드백 (없으면 null)

---

## **GET `/api/users/{userId}/daily`**

특정 날짜의 요약을 조회한다. 새 기록을 생성하지 않고 현재 상태만 읽는다.

- 인증: 필요
- 접근 권한: 본인만 가능
- 권한 조건: `#userId == principal`

### **Query 파라미터**

- `date`: 조회 날짜 (yyyy-MM-dd, 필수)

### **Response 200 OK**

```json
{
  "date": "2026-07-10",
  "totalScore": 45.0,
  "progress": 0.75,
  "statScores": { "physical": 10.0, "knowledge": 20.0 },
  "completedMissions": ["독서"],
  "remainingMissions": ["운동", "가계부 작성"],
  "aiFeedback": null
}
```

### **응답 필드**

- `date`: 대상 날짜
- `totalScore`: 그날 총점
- `progress`: 진행률 (0~1)
- `statScores`: 스탯별 점수 (키는 소문자 statType)
- `completedMissions`: 완료한 미션명 목록
- `remainingMissions`: 미완료 미션명 목록
- `aiFeedback`: 그날 이미 생성된 AI 피드백 (없으면 null)

---

## **POST `/api/users/{userId}/ai-feedback`**

그날 누적된 점수/완료 미션을 바탕으로 AI 피드백을 새로 생성한다(기존 피드백 덮어씀).

- 인증: 필요
- 접근 권한: 본인만 가능
- 권한 조건: `#userId == principal`

### **Query 파라미터**

- `date`: 대상 날짜 (yyyy-MM-dd, 필수)

### **Response 200 OK**

```json
{
  "summary": "오늘 전반적으로 꾸준히 목표를 향해 나아갔어요.",
  "praise": "독서 목표를 완전히 달성했어요!",
  "improvement": "운동 시간을 조금 더 늘려보면 좋겠어요.",
  "tomorrow": "내일은 가계부 작성도 함께 도전해봐요.",
  "cheer": "당신은 이미 충분히 잘하고 있어요 :)"
}
```

### **응답 필드**

- `summary`: 전체 요약
- `praise`: 칭찬
- `improvement`: 개선점
- `tomorrow`: 내일 제안
- `cheer`: 응원 문구

---

## **GET `/api/users/{userId}/weekly-report`**

`weekStart` 포함 7일을 집계한 주간 리포트를 조회한다.

- 인증: 필요
- 접근 권한: 본인만 가능
- 권한 조건: `#userId == principal`

### **Query 파라미터**

- `weekStart`: 주 시작일 (yyyy-MM-dd, 필수. 해당일 포함 7일 집계)

### **Response 200 OK**

```json
{
  "weekStart": "2026-07-06",
  "weekEnd": "2026-07-12",
  "totalScore": 210.0,
  "averageProgress": 0.68,
  "statScoreTotals": { "physical": 70.0, "knowledge": 140.0 },
  "missionCompletionCounts": { "독서": 5, "운동": 3 },
  "dailyBreakdown": [
    { "date": "2026-07-06", "totalScore": 30.0, "progress": 0.6, "statScores": {}, "completedMissions": [], "remainingMissions": [], "aiFeedback": null }
  ]
}
```

### **응답 필드**

- `weekStart`: 주 시작일
- `weekEnd`: 주 종료일 (weekStart+6)
- `totalScore`: 7일 총점 합계
- `averageProgress`: 평균 진행률 (기록 없는 날도 0으로 계산, 항상 7로 나눔)
- `statScoreTotals`: 스탯별 7일 합계
- `missionCompletionCounts`: 미션별 완료 횟수
- `dailyBreakdown`: 일자별 상세 (각 항목의 `aiFeedback`은 항상 null)

---

## **POST `/api/uploads`**

파일을 업로드한다 (`multipart/form-data`, 필드명 `file`, 최대 10MB).

- 인증: 필요
- 접근 권한: 로그인한 사용자 누구나

### **Response 200 OK**

```json
{ "url": "/uploads/3f2a-....jpg" }
```

### **응답 필드**

- `url`: 저장된 파일 URL

---

## **POST `/api/vision/analyze`**

업로드한 사진이 인증하려는 행동과 관련 있어 보이는지 분석한다 (`multipart/form-data`).

- 인증: 필요
- 접근 권한: 로그인한 사용자 누구나

### **Request 필드**

- `file`: 인증 사진
- `expectedTopic`: 인증 대상 행동/미션명 (선택, 기본값 "독서")

### **Response 200 OK**

```json
{ "likelyValid": true, "reason": "업로드된 이미지가 독서와 관련된 것으로 보입니다.", "confidence": "medium" }
```

### **응답 필드**

- `likelyValid`: 사진이 해당 주제와 관련 있어 보이는지
- `reason`: 판단 사유 텍스트
- `confidence`: 신뢰도 (`low` | `medium`)

---

## 전체 엔드포인트

| Method | Path | 인증 | 접근 권한 | 설명 |
|---|---|---|---|---|
| POST | `/api/auth/login` | 불필요 | 공개 | 닉네임 로그인/가입 |
| GET | `/api/users/{id}` | 필요 | 본인만 | 사용자 조회 |
| PUT | `/api/users/{id}/stat-focus` | 필요 | 본인만 | 스탯 비중 설정 (전체 교체) |
| GET | `/api/missions` | 필요 | 로그인 사용자 누구나 | 미션 카탈로그 조회 |
| POST | `/api/records` | 필요 | 본인만 | 행동 기록 입력(upsert) + 점수 재계산 |
| GET | `/api/users/{userId}/daily` | 필요 | 본인만 | 특정일 요약 조회 |
| POST | `/api/users/{userId}/ai-feedback` | 필요 | 본인만 | AI 피드백 생성 |
| GET | `/api/users/{userId}/weekly-report` | 필요 | 본인만 | 주간 리포트 조회 |
| POST | `/api/uploads` | 필요 | 로그인 사용자 누구나 | 파일 업로드 |
| POST | `/api/vision/analyze` | 필요 | 로그인 사용자 누구나 | 인증 사진 분석 |
