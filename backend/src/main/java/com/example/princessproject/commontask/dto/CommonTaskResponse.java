package com.example.princessproject.commontask.dto;

import com.example.princessproject.commontask.model.CommonTaskRecord;
import com.example.princessproject.commontask.model.CommonTaskType;
import com.example.princessproject.common.PhotoUrlListCodec;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CommonTaskResponse(
        Long id,
        CommonTaskType taskType,
        LocalDate recordDate,
        String bookTitle,
        Integer startPage,
        Integer endPage,
        BigDecimal studyPlannedAmount,
        BigDecimal studyCompletedAmount,
        String studyYoutubeUrl,
        String studyTakeaway,
        String photoUrl,
        Boolean aiVerified,
        String memo,
        LocalDateTime createdAt,
        List<String> extraPhotoUrls
) {
    public static CommonTaskResponse from(CommonTaskRecord record) {
        return new CommonTaskResponse(
                record.getId(),
                record.getTaskType(),
                record.getRecordDate(),
                record.getBookTitle(),
                record.getStartPage(),
                record.getEndPage(),
                record.getStudyPlannedAmount(),
                record.getStudyCompletedAmount(),
                record.getStudyYoutubeUrl(),
                record.getStudyTakeaway(),
                record.getPhotoUrl(),
                record.getAiVerified(),
                record.getMemo(),
                record.getCreatedAt(),
                PhotoUrlListCodec.decode(record.getExtraPhotoUrls())
        );
    }
}
