package com.example.princessproject.record.service;

import java.time.LocalDate;

public record DailyProgress(LocalDate date, MissionProgress progress) {
}
