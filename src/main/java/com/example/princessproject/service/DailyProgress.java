package com.example.princessproject.service;

import java.time.LocalDate;

public record DailyProgress(LocalDate date, MissionProgress progress) {
}
