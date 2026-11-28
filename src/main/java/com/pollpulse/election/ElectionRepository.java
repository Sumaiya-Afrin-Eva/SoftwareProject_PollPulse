package com.pollpulse.election;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionRepository extends JpaRepository<Election, Long> {

    Page<Election> findByStatus(ElectionStatus status, Pageable pageable);

    Page<Election> findByStatusIn(Collection<ElectionStatus> statuses, Pageable pageable);

    /** Elections in the given status whose start time has been reached (used to open them). */
    List<Election> findByStatusAndStartTimeLessThanEqual(ElectionStatus status, LocalDateTime time);

    /** Elections in the given status whose end time has been reached (used to close them). */
    List<Election> findByStatusAndEndTimeLessThanEqual(ElectionStatus status, LocalDateTime time);
}
