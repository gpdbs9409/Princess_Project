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

        List<AdminMemberWeekResponse> eligible = adminService.listParticipantsForWeek(null, weekStart).stream()
                .filter(AdminMemberWeekResponse::eligible)
                .toList();
        List<ValueRange> updates = new ArrayList<>();
        Set<String> alreadyFilled = new LinkedHashSet<>();
        Set<String> missing = new LinkedHashSet<>();

        for (AdminMemberWeekResponse member : eligible) {
            Integer rowIndex = rowByNickname.get(normalize(member.nickname()));
            if (rowIndex == null) {
                missing.add(member.nickname());
                continue;
            }
            String current = cell(rows.get(rowIndex), weekColumn).trim();
            if (!current.isEmpty()) {
                alreadyFilled.add(member.nickname());
                continue;
            }
            String range = quotedTab + "!" + columnName(weekColumn) + (rowIndex + 1);
            updates.add(new ValueRange().setRange(range).setValues(List.of(List.of(PAYBACK_AMOUNT))));
        }

        if (!updates.isEmpty()) {
            sheets.spreadsheets().values().batchUpdate(spreadsheetId,
                    new BatchUpdateValuesRequest()
                            .setValueInputOption("USER_ENTERED")
                            .setData(updates))
                    .execute();
        }
        return new PaybackSheetSyncResponse(weekStart, targetWeekHeader, eligible.size(), updates.size(),
                List.copyOf(alreadyFilled), List.copyOf(missing));
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
