package com.example.princessproject.record.service;

import java.util.Map;

/** Separate maps avoid id collisions between daily_records and daily_common_task_records. */
public record CurrentActivityScores(
        Map<Long, ActivityScoreSnapshot> personal,
        Map<Long, ActivityScoreSnapshot> common
) {
}
