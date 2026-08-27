-- 독서/공부 공통과제 사진의 Vision API 판정 결과 저장.
-- 판정 false는 저장 거부가 아니라 운영진 확인용 플래그다. 기존 행은 아직 판정 기록이 없으므로 NULL.

USE princess_project;

ALTER TABLE common_task_records
    ADD COLUMN ai_verified BOOLEAN NULL AFTER photo_url;
