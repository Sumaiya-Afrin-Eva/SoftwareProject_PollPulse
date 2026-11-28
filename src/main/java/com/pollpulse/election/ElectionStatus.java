package com.pollpulse.election;

/**
 * Lifecycle of an election: DRAFT -> SCHEDULED -> OPEN -> CLOSED -> PUBLISHED.
 */
public enum ElectionStatus {
    DRAFT,
    SCHEDULED,
    OPEN,
    CLOSED,
    PUBLISHED
}
