package com.pollpulse.election;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import com.pollpulse.candidate.Candidate;
import com.pollpulse.candidate.CandidateRepository;
import com.pollpulse.candidate.CandidateStatus;
import com.pollpulse.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ElectionEntitiesRepositoryTest {

    private static final LocalDateTime START = LocalDateTime.of(2030, 1, 10, 9, 0);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    private User commissioner;

    @BeforeEach
    void setUp() {
        commissioner = entityManager.persist(newUser("Election Commissioner", "commissioner@pollpulse.test"));
    }

    @Test
    void shouldRejectPosition_whenTitleIsDuplicatedInSameElection() {
        Election election = electionRepository.saveAndFlush(newElection("Student Council 2030"));
        positionRepository.saveAndFlush(new Position(election, "President", 1));

        assertThat(positionRepository.existsByElectionIdAndTitleIgnoreCase(election.getId(), "president")).isTrue();
        assertThat(positionRepository.countByElectionId(election.getId())).isEqualTo(1);

        assertThatThrownBy(() -> positionRepository.saveAndFlush(new Position(election, "President", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectCandidate_whenUserIsNominatedTwiceForSamePosition() {
        User student = entityManager.persist(newUser("Campus Student", "student@pollpulse.test"));
        Election election = electionRepository.saveAndFlush(newElection("Hall Committee 2030"));
        Position position = positionRepository.saveAndFlush(new Position(election, "Secretary", 1));
        candidateRepository.saveAndFlush(new Candidate(student, position, "Better study rooms"));

        assertThat(candidateRepository.existsByUserIdAndPositionElectionId(student.getId(), election.getId()))
                .isTrue();
        assertThat(candidateRepository.findByPositionIdAndStatus(position.getId(), CandidateStatus.PENDING))
                .hasSize(1);

        assertThatThrownBy(() -> candidateRepository.saveAndFlush(
                        new Candidate(student, position, "Second nomination")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldDefaultToDraftAndSingleChoice_whenElectionIsCreated() {
        Election saved = electionRepository.saveAndFlush(newElection("Club Election 2030"));
        entityManager.clear();

        Election loaded = electionRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(ElectionStatus.DRAFT);
        assertThat(loaded.getBallotType()).isEqualTo(BallotType.SINGLE_CHOICE);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(electionRepository.findByStatus(ElectionStatus.DRAFT, PageRequest.of(0, 10)).getContent())
                .extracting(Election::getId)
                .containsExactly(saved.getId());
    }

    private Election newElection(String title) {
        return new Election(title, "Annual election", START, START.plusHours(8), commissioner);
    }

    private static User newUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("not-a-real-hash");
        return user;
    }
}
