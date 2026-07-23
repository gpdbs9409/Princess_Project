package com.example.princessproject.common;

/**
 * A machine-readable code alongside a human message, so clients can branch on
 * exactly why a request failed instead of showing one generic error for everything.
 */
public record ApiErrorResponse(String code, String message) {
}
