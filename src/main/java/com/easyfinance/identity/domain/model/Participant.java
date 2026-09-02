package com.easyfinance.identity.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.util.Objects;

public final class Participant {

    private final Long id;
    private final Long userId;
    private final String displayName;
    private final ParticipantStatus status;

    private Participant(Long id, Long userId, String displayName, ParticipantStatus status) {
        this.id = id;
        this.userId = userId;
        this.displayName = requireText(displayName);
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static Participant createForUser(Long userId, String displayName) {
        if (userId == null) {
            throw new BusinessRuleViolationException("USER_ID_REQUIRED", "User id is required.");
        }
        return new Participant(null, userId, displayName, ParticipantStatus.ACTIVE);
    }

    public static Participant restore(Long id, Long userId, String displayName, ParticipantStatus status) {
        return new Participant(id, userId, displayName, status);
    }

    public Participant rename(String displayName) {
        return new Participant(id, userId, displayName, status);
    }

    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public String displayName() {
        return displayName;
    }

    public ParticipantStatus status() {
        return status;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException("DISPLAY_NAME_REQUIRED", "Display name is required.");
        }
        return value.trim();
    }
}

