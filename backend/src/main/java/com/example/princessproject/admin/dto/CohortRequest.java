package com.example.princessproject.admin.dto;

/** cohort == null/blank clears the tag (moves the member back to the applicant list). */
public record CohortRequest(String cohort) {
}
