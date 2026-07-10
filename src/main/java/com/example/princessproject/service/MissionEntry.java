package com.example.princessproject.service;

import com.example.princessproject.domain.MissionDefinition;

/**
 * inputValue is null when the mission hasn't been submitted for the date yet.
 */
public record MissionEntry(MissionDefinition mission, Double inputValue) {
}
