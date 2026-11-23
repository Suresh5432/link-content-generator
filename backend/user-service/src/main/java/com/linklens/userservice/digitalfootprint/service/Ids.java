package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

final class Ids {
    private Ids() {}

    static UUID parseUuid(String value, String field) {
        if (value == null || value.isBlank()) throw new InvalidRequestException(field + " is required");
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(field + " must be a valid UUID");
        }
    }

    static UUID parseNullableUuid(String value, String field) {
        if (value == null || value.isBlank()) return null;
        return parseUuid(value, field);
    }

    static LocalDate parseDate(String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException(field + " must be an ISO date (YYYY-MM-DD)");
        }
    }

    static Instant parseInstant(String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException(field + " must be an ISO-8601 instant");
        }
    }
}
