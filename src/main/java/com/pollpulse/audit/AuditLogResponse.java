package com.pollpulse.audit;

import java.time.LocalDateTime;

/**
 * Read model for the audit list. The raw hashes are not exposed because they are
 * only useful to {@code GET /api/audit/verify}.
 */
public record AuditLogResponse(
        Long id,
        String actorName,
        AuditAction action,
        String entityType,
        Long entityId,
        String details,
        LocalDateTime createdAt) {
}
