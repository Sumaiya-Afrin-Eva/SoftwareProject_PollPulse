package com.pollpulse.candidate;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    List<Candidate> findByPositionId(Long positionId);

    List<Candidate> findByPositionIdAndStatus(Long positionId, CandidateStatus status);

    List<Candidate> findByUserId(Long userId);

    /** True when the user is already a candidate for any position in the election. */
    boolean existsByUserIdAndPositionElectionId(Long userId, Long electionId);

    boolean existsByPositionId(Long positionId);
}
