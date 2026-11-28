package com.pollpulse.audit;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.Repository;

/**
 * Deliberately extends {@link Repository} instead of {@code JpaRepository}: the
 * reduced interface has no {@code delete} or {@code deleteAll}, so application
 * code has no way to remove an audit record. Only the methods the audit module
 * actually needs are declared here.
 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    <S extends AuditLog> S save(S entity);

    Optional<AuditLog> findTopByOrderByIdDesc();

    List<AuditLog> findAllByOrderByIdAsc();

    Page<AuditLog> findAll(Specification<AuditLog> specification, Pageable pageable);
}
