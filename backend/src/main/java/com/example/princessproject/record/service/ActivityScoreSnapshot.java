package com.example.princessproject.record.service;

import java.math.BigDecimal;

/** Current scoring-policy result for one stored activity, keyed by that activity's record id. */
public record ActivityScoreSnapshot(BigDecimal earnedScore, BigDecimal achievementRate) {
}
