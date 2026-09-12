package com.example.princessproject.admin.service;

import com.example.princessproject.admin.dto.AdminMemberWeekResponse;
import com.example.princessproject.admin.dto.PaybackSheetSyncResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Writes eligible participants into the existing payback sheet without replacing operator data. */
@Service
public class PaybackSheetService {

    private static final Logger log = LoggerFactory.getLogger(PaybackSheetService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final String NICKNAME_HEADER = "닉네임";
    private static final int PAYBACK_AMOUNT = 25_000;

    /**
     * 어드민에서 "환급하기"를 눌러 개별 참가자 한 명을 시트에 즉시 반영한 결과 (2026-09).
     * 매주 월요일 자동 배치 동기화({@link #sync})와 달리, 이건 관리자가 그 자리에서 지급 처리한
     * 인원 한 명만 반영하는 경로다. 시트 장애/미설정이 지급 처리(DB 저장) 자체를 막아서는 안 되므로
     * 예외를 던지지 않고 이 결과값으로만 상태를 알린다 - 호출부(AdminController)가 이 값을 응답에
     * 실어 프론트에 보여준다.
     */
    public enum SheetSyncOutcome {
        /** 시트에 새로 기록했다. */
        WRITTEN,
        /** 이미 값이 있어 건드리지 않았다 (기존 정책: 운영자가 수기로 채운 값을 덮어쓰지 않는다). */
        ALREADY_FILLED,
        /** 이 닉네임으로 시트에서 행을 찾지 못했다. */
        NOT_FOUND_IN_SHEET,
        /** 시트 연동 자체가 꺼져 있거나 설정되지 않았다. */
        DISABLED,
        /** 시트 API 호출 중 오류가 발생했다 (네트워크, 인증, 스키마 등). */
        ERROR
    }

    private final AdminService adminService;
    private final boolean enabled;
    private final String spreadsheetId;
    private final String tabName;
    private final LocalDate challengeStart;
    private final String credentialsBase64;

    public PaybackSheetService(
            AdminService adminService,
            @Value("${payback.google-sheet.enabled:false}") boolean enabled,
            @Value("${payback.google-sheet.spreadsheet-id:}") String spreadsheetId,
            @Value("${payback.google-sheet.tab-name:환급}") String tabName,
            @Value("${payback.google-sheet.challenge-start:2026-09-01}") LocalDate challengeStart,
            @Value("${payback.google-sheet.credentials-base64:}") String credentialsBase64
    ) {
        this.adminService = adminService;
        this.enabled = enabled;
        this.spreadsheetId = spreadsheetId;
        this.tabName = tabName;
        this.challengeStart = challengeStart;
        this.credentialsBase64 = credentialsBase64;
    }

    /** Monday 09:00 KST: export the week that ended the previous day. */
    @Scheduled(cron = "${payback.google-sheet.cron:0 0 9 * * MON}", zone = "Asia/Seoul")
    public void syncPreviousWeekOnMonday() {
        if (!enabled) {
            return;
        }
        LocalDate previousWeekStart = LocalDate.now(SEOUL).minusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        try {
            PaybackSheetSyncResponse result = sync(previousWeekStart);
            log.info("Payback sheet sync complete: weekStart={}, eligible={}, written={}, missing={}",
                    result.weekStart(), result.eligibleCount(), result.writtenCount(), result.missingNicknames());
        } catch (Exception exception) {
            // A sheet outage must never stop the application or alter refund calculations in MySQL.
            log.error("Payback sheet sync failed for week starting {}", previousWeekStart, exception);
        }
    }

    public PaybackSheetSyncResponse sync(LocalDate weekStart) throws IOException, GeneralSecurityException {
        SheetContext ctx = openContext(weekStart);

        List<AdminMemberWeekResponse> eligible = adminService.listParticipantsForWeek(null, weekStart).stream()
                .filter(AdminMemberWeekResponse::eligible)
                .toList();
        List<ValueRange> updates = new ArrayList<>();
        Set<String> alreadyFilled = new LinkedHashSet<>();
        Set<String> missing = new LinkedHashSet<>();

        for (AdminMemberWeekResponse member : eligible) {
            Integer rowIndex = ctx.rowByNickname.get(normalize(member.nickname()));
            if (rowIndex == null) {
                missing.add(member.nickname());
                continue;
            }
            String current = cell(ctx.rows.get(rowIndex), ctx.weekColumn).trim();
            if (!current.isEmpty()) {
                alreadyFilled.add(member.nickname());
                continue;
            }
            String range = ctx.quotedTab + "!" + columnName(ctx.weekColumn) + (rowIndex + 1);
            updates.add(new ValueRange().setRange(range).setValues(List.of(List.of(PAYBACK_AMOUNT))));
        }

        if (!updates.isEmpty()) {
            ctx.sheets.spreadsheets().values().batchUpdate(spreadsheetId,
                    new BatchUpdateValuesRequest()
                            .setValueInputOption("USER_ENTERED")
                            .setData(updates))
                    .execute();
        }
        return new PaybackSheetSyncResponse(weekStart, ctx.targetWeekHeader, eligible.size(), updates.size(),
                List.copyOf(alreadyFilled), List.copyOf(missing));
    }

    /**
     * 어드민에서 참가자 한 명을 "환급하기" 처리하는 즉시 그 자리에서 시트에 반영한다. DB 지급 상태
     * 저장(AdminService.setRefundPaid)이 이미 끝난 뒤에 호출되는 부가 동작이므로, 여기서 어떤
     * 예외가 나든 절대 위로 던지지 않고 outcome으로만 알린다 - 시트가 막혀도 지급 처리 자체는
     * 이미 끝나 있어야 한다.
     */
    public SheetSyncOutcome syncSingleMember(String nickname, LocalDate weekStart) {
        if (!enabled) {
            return SheetSyncOutcome.DISABLED;
        }
        try {
            SheetContext ctx = openContext(weekStart);
            Integer rowIndex = ctx.rowByNickname.get(normalize(nickname));
            if (rowIndex == null) {
                return SheetSyncOutcome.NOT_FOUND_IN_SHEET;
            }
            String current = cell(ctx.rows.get(rowIndex), ctx.weekColumn).trim();
            if (!current.isEmpty()) {
                return SheetSyncOutcome.ALREADY_FILLED;
            }
            String range = ctx.quotedTab + "!" + columnName(ctx.weekColumn) + (rowIndex + 1);
            ctx.sheets.spreadsheets().values()
                    .update(spreadsheetId, range, new ValueRange().setValues(List.of(List.of(PAYBACK_AMOUNT))))
                    .setValueInputOption("USER_ENTERED")
                    .execute();
            return SheetSyncOutcome.WRITTEN;
        } catch (Exception exception) {
            log.error("Single-member payback sheet sync failed: nickname={}, weekStart={}", nickname, weekStart,
                    exception);
            return SheetSyncOutcome.ERROR;
        }
    }

    private record SheetContext(
            Sheets sheets, List<List<Object>> rows, int nicknameColumn, int weekColumn,
            Map<String, Integer> rowByNickname, String quotedTab, String targetWeekHeader
    ) {
    }

    private SheetContext openContext(LocalDate weekStart) throws IOException, GeneralSecurityException {
        requireConfigured();
        String targetWeekHeader = resolveWeekHeader(weekStart);
        Sheets sheets = createSheetsClient();
        String quotedTab = "'" + tabName.replace("'", "''") + "'";
        List<List<Object>> rows = sheets.spreadsheets().values()
                .get(spreadsheetId, quotedTab + "!A:Z")
                .execute()
                .getValues();
        if (rows == null || rows.isEmpty()) {
            throw new IllegalStateException("환급 시트가 비어 있습니다.");
        }

        List<Object> header = rows.getFirst();
        int nicknameColumn = findHeaderColumn(header, NICKNAME_HEADER);
        int weekColumn = findWeekColumn(header, targetWeekHeader);

        Map<String, Integer> rowByNickname = new HashMap<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            String nickname = cell(rows.get(rowIndex), nicknameColumn).trim();
            if (!nickname.isEmpty()) {
                rowByNickname.put(normalize(nickname), rowIndex);
            }
        }
        return new SheetContext(sheets, rows, nicknameColumn, weekColumn, rowByNickname, quotedTab, targetWeekHeader);
    }

    private void requireConfigured() {
        if (!enabled) {
            throw new IllegalStateException("PAYBACK_SHEET_ENABLED가 꺼져 있습니다.");
        }
        if (spreadsheetId.isBlank() || credentialsBase64.isBlank()) {
            throw new IllegalStateException("Google Sheets 연동 환경변수가 설정되지 않았습니다.");
        }
    }

    private Sheets createSheetsClient() throws IOException, GeneralSecurityException {
        byte[] json;
        try {
            json = Base64.getDecoder().decode(credentialsBase64.replaceAll("\\s", ""));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("PAYBACK_GOOGLE_CREDENTIALS_BASE64 값이 올바른 Base64가 아닙니다.", exception);
        }
        GoogleCredentials credential = GoogleCredentials
                .fromStream(new ByteArrayInputStream(json))
                .createScoped(List.of(SheetsScopes.SPREADSHEETS));
        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credential))
                .setApplicationName("Princess Project Payback")
                .build();
    }

    private String resolveWeekHeader(LocalDate weekStart) {
        LocalDate firstWeekStart = challengeStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long weekIndex = ChronoUnit.WEEKS.between(firstWeekStart, weekStart);
        if (weekIndex < 0) {
            throw new IllegalArgumentException("챌린지 시작 전 주차는 연동할 수 없습니다: " + weekStart);
        }
        return (weekIndex + 1) + "주";
    }

    private int findHeaderColumn(List<Object> header, String exactHeader) {
        for (int index = 0; index < header.size(); index++) {
            if (exactHeader.equals(cell(header, index).trim())) {
                return index;
            }
        }
        throw new IllegalStateException("환급 시트에서 '" + exactHeader + "' 열을 찾지 못했습니다.");
    }

    private int findWeekColumn(List<Object> header, String weekHeaderPrefix) {
        for (int index = 0; index < header.size(); index++) {
            if (cell(header, index).trim().startsWith(weekHeaderPrefix)) {
                return index;
            }
        }
        throw new IllegalStateException("환급 시트에서 '" + weekHeaderPrefix + "' 주차 열을 찾지 못했습니다.");
    }

    private String cell(List<Object> row, int column) {
        if (row == null || column < 0 || column >= row.size() || row.get(column) == null) {
            return "";
        }
        return String.valueOf(row.get(column));
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String columnName(int zeroBasedColumn) {
        int number = zeroBasedColumn + 1;
        StringBuilder result = new StringBuilder();
        while (number > 0) {
            int remainder = (number - 1) % 26;
            result.append((char) ('A' + remainder));
            number = (number - 1) / 26;
        }
        return result.reverse().toString();
    }
}
