package com.example.princessproject.common;

import java.util.List;

/** One canonical spelling prevents legacy "1" and current "1기" from splitting a cohort. */
public final class CohortNames {
    private CohortNames() {}

    public static String canonical(String cohort) {
        if (cohort == null || cohort.isBlank()) return null;
        String value = cohort.trim();
        return value.matches("\\d+") ? value + "기" : value;
    }

    public static List<String> aliases(String cohort) {
        String value = canonical(cohort);
        if (value == null) return List.of();
        if (value.matches("\\d+기")) return List.of(value, value.substring(0, value.length() - 1));
        return List.of(value);
    }
}
