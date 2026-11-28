package com.pollpulse.audit;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import com.pollpulse.exception.BadRequestException;
import com.pollpulse.exception.PageResponse;
import com.pollpulse.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only, hash-chained audit trail.
 *
 * <p>Each record stores the hash of the previous record, so the records form a
 * chain: editing or deleting any row changes the recomputed hash of that row (and
 * of every row after it), which {@link #verifyChain()} reports.
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    /** Previous hash used by the very first record of the chain. */
    public static final String GENESIS = "GENESIS";

    private static final String SYSTEM_ACTOR = "SYSTEM";
    private static final int MAX_DETAILS_LENGTH = 1000;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Fixed pattern so a timestamp read back from the database renders exactly like
     * the value that was hashed. Timestamps are truncated to milliseconds before
     * hashing because that is the precision the database keeps.
     */
    private static final DateTimeFormatter HASH_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AuditService(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    /**
     * Appends one record to the chain.
     *
     * <p>{@code synchronized} stops two threads of this instance from reading the
     * same previous hash and branching the chain. The monitor covers a single
     * application instance; when a caller already runs inside a transaction (for
     * example the vote facade) that caller's own lock is the serialisation point.
     *
     * <p>This method intentionally has no {@code @Transactional} of its own so a
     * standalone call commits immediately, while a call made from inside another
     * service joins that service's transaction and stays atomic with it.
     */
    public synchronized AuditLog log(
            User actor, AuditAction action, String entityType, Long entityId, String details) {
        LocalDateTime createdAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        String previousHash = auditLogRepository.findTopByOrderByIdDesc()
                .map(AuditLog::getLogHash)
                .orElse(GENESIS);
        String safeDetails = truncate(details);
        String logHash = computeHash(
                previousHash,
                actor == null ? null : actor.getId(),
                action,
                entityType,
                entityId,
                safeDetails,
                createdAt);

        AuditLog record = new AuditLog(
                actor, action, entityType, entityId, safeDetails, createdAt, previousHash, logHash);
        AuditLog saved = auditLogRepository.save(record);
        logger.debug("Audit record appended: action={} entityType={} entityId={}",
                action, entityType, entityId);
        return saved;
    }

    /**
     * Recomputes every hash in id order and returns the first record that does not
     * match. An empty chain is considered valid.
     */
    @Transactional(readOnly = true)
    public ChainVerification verifyChain() {
        List<AuditLog> records = auditLogRepository.findAllByOrderByIdAsc();
        String expectedPreviousHash = GENESIS;
        long checked = 0;

        for (AuditLog record : records) {
            checked++;
            boolean linkBroken = !expectedPreviousHash.equals(record.getPreviousHash());
            String recomputed = computeHash(
                    record.getPreviousHash(),
                    record.getActor() == null ? null : record.getActor().getId(),
                    record.getAction(),
                    record.getEntityType(),
                    record.getEntityId(),
                    record.getDetails(),
                    record.getCreatedAt());
            boolean contentBroken = !recomputed.equals(record.getLogHash());

            if (linkBroken || contentBroken) {
                logger.warn("Audit chain broken at record id={} after {} checked records",
                        record.getId(), checked);
                return new ChainVerification(false, record.getId(), checked);
            }
            expectedPreviousHash = record.getLogHash();
        }

        return new ChainVerification(true, null, checked);
    }

    /**
     * Paged, newest first, with optional filters. The specification is built here
     * rather than in the controller so the controller stays a thin adapter.
     */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(
            String action,
            String entityType,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
        if (page < 0) {
            throw new BadRequestException("Page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }

        AuditAction auditAction = parseAction(action);
        Specification<AuditLog> specification = Specification.where(null);

        if (auditAction != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(root.get("action"), auditAction));
        }
        if (entityType != null && !entityType.isBlank()) {
            String value = entityType.trim();
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(root.get("entityType"), value));
        }
        if (from != null) {
            LocalDateTime start = from.atStartOfDay();
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), start));
        }
        if (to != null) {
            // The upper bound is the start of the following day so the whole `to`
            // date is included.
            LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.lessThan(root.get("createdAt"), endExclusive));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<AuditLog> result = auditLogRepository.findAll(specification, pageable);
        return PageResponse.from(result.map(this::toResponse));
    }

    private AuditLogResponse toResponse(AuditLog record) {
        String actorName = record.getActor() == null ? SYSTEM_ACTOR : record.getActor().getName();
        return new AuditLogResponse(
                record.getId(),
                actorName,
                record.getAction(),
                record.getEntityType(),
                record.getEntityId(),
                record.getDetails(),
                record.getCreatedAt());
    }

    private AuditAction parseAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        try {
            return AuditAction.valueOf(action.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unknown audit action: " + action);
        }
    }

    /**
     * Single source of truth for the hash payload. {@link #log} and
     * {@link #verifyChain} both call this, so the chain stays recomputable from the
     * stored columns alone.
     */
    private String computeHash(
            String previousHash,
            Long actorId,
            AuditAction action,
            String entityType,
            Long entityId,
            String details,
            LocalDateTime createdAt) {
        String payload = String.join("|",
                previousHash,
                actorId == null ? SYSTEM_ACTOR : String.valueOf(actorId),
                action.name(),
                entityType == null ? "" : entityType,
                entityId == null ? "" : String.valueOf(entityId),
                details == null ? "" : details,
                createdAt.format(HASH_TIMESTAMP_FORMAT));
        return HashUtil.sha256Hex(payload);
    }

    private String truncate(String details) {
        if (details == null) {
            return null;
        }
        return details.length() <= MAX_DETAILS_LENGTH
                ? details
                : details.substring(0, MAX_DETAILS_LENGTH);
    }
}
