#!/usr/bin/env bash
# 프린세스 다이어리 - MySQL 백업 스크립트
#
# 사용법:
#   1) Railway → MySQL 서비스 → Variables 탭에서 MYSQL_PUBLIC_URL 값을 복사
#   2) 아래처럼 실행
#        MYSQL_PUBLIC_URL='mysql://user:pass@host:port/railway' ./scripts/backup-db.sh
#
#   매번 붙여넣기 귀찮으면 ~/.zshrc 에 export 해두면 됩니다.
#
# 필요 도구: mysqldump  (없으면 → brew install mysql-client)

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-$HOME/princess-backups}"
KEEP="${KEEP:-14}"   # 최근 몇 개를 남길지

if [[ -z "${MYSQL_PUBLIC_URL:-}" ]]; then
  echo "❌ MYSQL_PUBLIC_URL 이 설정되지 않았습니다."
  echo "   Railway → MySQL 서비스 → Variables → MYSQL_PUBLIC_URL 값을 복사해서 쓰세요."
  exit 1
fi

if ! command -v mysqldump >/dev/null 2>&1; then
  echo "❌ mysqldump 가 없습니다. 먼저 설치해주세요:"
  echo "   brew install mysql-client"
  echo "   echo 'export PATH=\"/opt/homebrew/opt/mysql-client/bin:\$PATH\"' >> ~/.zshrc"
  exit 1
fi

# mysql://user:pass@host:port/dbname 파싱
proto_removed="${MYSQL_PUBLIC_URL#mysql://}"
credentials="${proto_removed%%@*}"
hostpart="${proto_removed#*@}"

DB_USER="${credentials%%:*}"
DB_PASS="${credentials#*:}"
DB_HOST="${hostpart%%:*}"
rest="${hostpart#*:}"
DB_PORT="${rest%%/*}"
DB_NAME="${rest#*/}"
DB_NAME="${DB_NAME%%\?*}"

mkdir -p "$BACKUP_DIR"
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT="$BACKUP_DIR/princess_${STAMP}.sql.gz"

echo "▶ 백업 시작: $DB_NAME @ $DB_HOST:$DB_PORT"

# --single-transaction : 테이블을 잠그지 않고 일관된 스냅샷을 뜬다 (서비스 중단 없음)
# --routines --triggers: 프로시저·트리거까지 포함
mysqldump \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --user="$DB_USER" \
  --password="$DB_PASS" \
  --single-transaction \
  --routines \
  --triggers \
  --default-character-set=utf8mb4 \
  "$DB_NAME" | gzip > "$OUT"

SIZE="$(du -h "$OUT" | cut -f1)"
echo "✅ 완료: $OUT ($SIZE)"

# 백업 파일이 비정상적으로 작으면 실패했을 가능성이 높다
BYTES="$(wc -c < "$OUT" | tr -d ' ')"
if [[ "$BYTES" -lt 1024 ]]; then
  echo "⚠️  파일이 1KB 미만입니다. 백업이 제대로 안 됐을 수 있으니 내용을 확인하세요."
  exit 1
fi

# 오래된 백업 정리
cd "$BACKUP_DIR"
ls -1t princess_*.sql.gz 2>/dev/null | tail -n +$((KEEP + 1)) | while read -r old; do
  echo "🗑  오래된 백업 삭제: $old"
  rm -f "$old"
done

echo "📦 보관 중인 백업: $(ls -1 princess_*.sql.gz 2>/dev/null | wc -l | tr -d ' ')개"
