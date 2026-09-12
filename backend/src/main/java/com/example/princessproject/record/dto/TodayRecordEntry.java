package com.example.princessproject.record.dto;

import com.example.princessproject.common.PhotoUrlListCodec;
import com.example.princessproject.record.model.DailyRecord;
import java.math.BigDecimal;
import java.util.List;

public record TodayRecordEntry(
        BigDecimal inputValue, String memo, String photoUrl, Boolean aiVerified, List<String> extraPhotoUrls
) {

    public static TodayRecordEntry from(DailyRecord record) {
        return new TodayRecordEntry(record.getInputValue(), record.getMemo(), record.getPhotoUrl(),
                record.getAiVerified(), PhotoUrlListCodec.decode(record.getExtraPhotoUrls()));
    }
}
