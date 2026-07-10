package com.example.princessproject.record.service;

import com.example.princessproject.mission.model.MissionDefinition;

/**
 * inputValue is null when the mission hasn't been submitted for the date yet.
 */
public record MissionEntry(MissionDefinition mission, Double inputValue) {
}
