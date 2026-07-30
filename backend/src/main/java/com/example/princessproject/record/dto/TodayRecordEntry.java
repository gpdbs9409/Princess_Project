package com.example.princessproject.record.dto;

import com.example.princessproject.record.model.DailyRecord;
import java.math.BigDecimal;

public record TodayRecordEntry(BigDecimal inputValue, String memo, String photoUrl, Boolean aiVerified) {

    public static TodayRecordEntry from(DailyRecord record) {
        return new TodayRecordEntry(record.getInputValue(), record.getMemo(), record.getPhotoUrl(), record.getAiVerified());
    }
}
