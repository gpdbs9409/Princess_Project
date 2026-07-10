# 배포 구성 (Vercel + Railway)

- 프론트: Vercel (`frontend/`)
- 백엔드: Railway, Spring Boot (`backend/`, Dockerfile)
- DB: Railway MySQL 플러그인
- 이미지: Railway Buckets (S3 호환 오브젝트 스토리지)

코드는 아래 구성으로 배포 준비가 되어 있지만, Railway/Vercel 프로젝트 생성·환경변수 입력·도메인 연결은 각 서비스의 계정/대시보드에서 직접 하셔야 합니다 (제가 대신 만들 수 없는 외부 SaaS 작업입니다).

## 1. Railway — MySQL

1. Railway 프로젝트에 **MySQL** 플러그인 추가.
2. 플러그인이 만들어주는 `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD` 변수를 백엔드 서비스의 환경변수로 참조 연결(Railway는 `${{MySQL.MYSQLHOST}}` 형태로 다른 서비스 변수를 참조 가능):
   - `DB_HOST=${{MySQL.MYSQLHOST}}`
   - `DB_PORT=${{MySQL.MYSQLPORT}}`
   - `DB_NAME=${{MySQL.MYSQLDATABASE}}`
   - `DB_USERNAME=${{MySQL.MYSQLUSER}}`
   - `DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}`

`backend/src/main/resources/application.properties`가 이미 이 변수들로 JDBC URL을 조립하도록 되어 있습니다:
```
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:princess_project}
```
테이블은 `ddl-auto=update`라 최초 기동 시 자동 생성됩니다 (별도 마이그레이션 불필요).

## 2. Railway — 백엔드 (Spring Boot)

1. 새 서비스 생성 → 이 GitHub 저장소 연결 → **Root Directory를 `backend`로 지정**.
2. `backend/Dockerfile`이 있으므로 Railway가 자동으로 Docker 빌드를 사용합니다 (JDK 21 툴체인 고정, Nixpacks의 자바 버전 자동 감지에 의존하지 않음).
3. 환경변수:
   | 변수 | 값 |
   |---|---|
   | `DB_HOST`,`DB_PORT`,`DB_NAME`,`DB_USERNAME`,`DB_PASSWORD` | 위 MySQL 섹션 참고 |
   | `JWT_SECRET` | 운영용 랜덤 시크릿 (기본값은 개발용이라 반드시 교체) |
   | `CORS_ALLOWED_ORIGINS` | Vercel 배포 URL (예: `https://princess-project.vercel.app`). 여러 개면 쉼표로 구분 |
   | `AWS_S3_BUCKET`,`AWS_REGION`,`AWS_S3_ENDPOINT` | 아래 버킷 섹션 참고 |
   | `AWS_ACCESS_KEY_ID`,`AWS_SECRET_ACCESS_KEY` | 버킷 자격증명 (AWS SDK 기본 자격증명 체인이 이 이름의 환경변수를 자동으로 읽음) |
   | `OPENAI_API_KEY` | 설정 시 실제 GPT-4o mini 피드백 생성, 비워두면 목(mock) 피드백 |
4. Railway가 주입하는 `PORT` 변수를 앱이 그대로 사용합니다 (`server.port=${PORT:8080}` 이미 반영됨) — 별도 설정 불필요.
5. 배포 후 Railway가 주는 공개 URL(`https://xxx.up.railway.app`)을 프론트의 `VITE_API_BASE_URL`로 사용하세요.

## 3. Railway — Buckets (이미지 저장)

Railway Buckets는 S3 호환이지만 **기본적으로 비공개**라 AWS S3처럼 공개 URL이 없습니다. 그래서 백엔드가 사진을 직접 프록시하도록 이미 바꿔뒀습니다:

- 업로드(`POST /api/uploads`) 시 반환되는 URL이 버킷의 실제 주소가 아니라 우리 서버의 `/api/uploads/{key}`이고, 이 엔드포인트가 버킷에서 파일을 읽어 그대로 스트리밍합니다.
- 이 GET 엔드포인트는 인증 없이 공개(`SecurityConfig`)로 열려 있습니다 — `<img src>`는 Authorization 헤더를 못 붙이기 때문입니다.

설정 방법:
1. Railway 프로젝트에서 **Bucket** 생성 (Create → Bucket).
2. 버킷이 제공하는 자격증명/엔드포인트를 백엔드 환경변수에 매핑:
   - `AWS_S3_BUCKET` = 버킷 이름
   - `AWS_S3_ENDPOINT` = 버킷이 제공하는 S3 엔드포인트 URL
   - `AWS_REGION` = 버킷이 안내하는 리전 (모르면 `auto`)
   - `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` = 버킷 자격증명
3. `AWS_S3_BUCKET`이 비어있으면 로컬 디스크(`./uploads`)를 쓰는 개발용 동작으로 자동 폴백됩니다 — 로컬 개발 시엔 아무 것도 설정할 필요 없음.

## 4. Vercel — 프론트엔드

1. Vercel에서 이 저장소 Import → **Root Directory를 `frontend`로 지정** (Vite 프로젝트 자동 인식).
2. Project Settings → Environment Variables:
   - `VITE_API_BASE_URL` = Railway 백엔드의 공개 URL (예: `https://princess-project-backend.up.railway.app`)
   - Vite는 빌드 시점에 env를 번들에 굽기 때문에, 로컬 `.env`가 아니라 **Vercel 대시보드의 환경변수**로 등록해야 배포본에 반영됩니다.
3. `frontend/vercel.json`에 SPA 라우팅용 rewrite가 이미 있어서, `/dashboard`처럼 새로고침해도 404 없이 `index.html`로 폴백됩니다 (React Router client-side routing).

## 5. 체크리스트

- [ ] Railway MySQL 플러그인 추가 + 백엔드 서비스에 `DB_*` 변수 연결
- [ ] Railway 백엔드 서비스 생성 (root: `backend`, Dockerfile 자동 인식) + 위 표의 환경변수 입력
- [ ] Railway Bucket 생성 + `AWS_S3_*` / `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` 연결
- [ ] Vercel 프로젝트 생성 (root: `frontend`) + `VITE_API_BASE_URL` 설정
- [ ] 백엔드 `CORS_ALLOWED_ORIGINS`에 실제 Vercel URL 반영
- [ ] `JWT_SECRET`을 운영용 랜덤 값으로 교체
