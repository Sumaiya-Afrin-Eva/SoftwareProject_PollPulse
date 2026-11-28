package com.pollpulse.election;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByElectionId(Long electionId);

    boolean existsByElectionIdAndTitleIgnoreCase(Long electionId, String title);

    long countByElectionId(Long electionId);
}
