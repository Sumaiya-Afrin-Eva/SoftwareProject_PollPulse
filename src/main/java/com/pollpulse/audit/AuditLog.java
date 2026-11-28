package com.pollpulse.audit;

import java.time.LocalDateTime;

import com.pollpulse.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One immutable audit record.
 *
 * <p>Every field is assigned by the constructor and the class deliberately exposes
 * no setters, so a record cannot be edited through the application. The log is
 * tamper-<em>evident</em> rather than tamper-proof: somebody with direct database
 * access can still change a row, but doing so breaks the hash chain and
 * {@link AuditService#verifyChain()} reports the first broken record.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null for system actions that have no human actor. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 1000)
    private String details;

    @Column(name = "log_hash", nullable = false, length = 64)
    private String logHash;

    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
        // Required by JPA.
    }

    public AuditLog(
            User actor,
            AuditAction action,
            String entityType,
            Long entityId,
            String details,
            LocalDateTime createdAt,
            String previousHash,
            String logHash) {
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.createdAt = createdAt;
        this.previousHash = previousHash;
        this.logHash = logHash;
    }

    public Long getId() {
        return id;
    }

    public User getActor() {
        return actor;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }

    public String getLogHash() {
        return logHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
