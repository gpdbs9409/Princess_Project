-- 인증 사진 여러 장 지원 (2026-09). 대표 사진(photo_url)은 그대로 AI 판정 대상이고,
-- 이 컬럼은 참고용 추가 증빙 사진 URL을 쉼표로 구분해 최대 4개까지 저장한다.
ALTER TABLE daily_records ADD COLUMN extra_photo_urls VARCHAR(2000) NULL;
ALTER TABLE daily_common_task_records ADD COLUMN extra_photo_urls VARCHAR(2000) NULL;
