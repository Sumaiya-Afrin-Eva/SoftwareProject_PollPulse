package com.pollpulse.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T10:15:30.123Z");

    @Mock(mockMaker = MockMakers.SUBCLASS)
    private AuditLogRepository auditLogRepository;

    private final List<AuditLog> stored = new ArrayList<>();
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        auditService = new AuditService(auditLogRepository, clock);
    }

    @Test
    void shouldUseGenesisAsPreviousHash_whenChainIsEmpty() {
        stubAppend();

        AuditLog first = auditService.log(
                null, AuditAction.ELECTION_CREATED, "Election", 7L, "Election created");

        assertThat(first.getPreviousHash()).isEqualTo(AuditService.GENESIS);
        assertThat(first.getLogHash()).hasSize(64);
        assertThat(first.getCreatedAt())
                .isEqualTo(LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));
    }

    @Test
    void shouldProduceTheSameHash_whenInputIsIdentical() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        AuditLog first = auditService.log(
                null, AuditAction.VOTE_CAST, "Election", 3L, "Ballot cast");
        AuditLog second = auditService.log(
                null, AuditAction.VOTE_CAST, "Election", 3L, "Ballot cast");

        assertThat(first.getLogHash()).isEqualTo(second.getLogHash());
    }

    @Test
    void shouldVerifyChain_whenRecordsAreUntampered() {
        stubAppend();
        stubReadAll();

        AuditLog first = auditService.log(
                null, AuditAction.ELECTION_CREATED, "Election", 1L, "Election created");
        AuditLog second = auditService.log(
                null, AuditAction.VOTE_CAST, "Election", 1L, "Ballot cast");
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);

        ChainVerification verification = auditService.verifyChain();

        assertThat(verification.valid()).isTrue();
        assertThat(verification.firstBrokenId()).isNull();
        assertThat(verification.checkedCount()).isEqualTo(2);
    }

    @Test
    void shouldDetectTamperedRecord_whenDetailsIsChanged() {
        stubAppend();
        stubReadAll();

        AuditLog first = auditService.log(
                null, AuditAction.ELECTION_CREATED, "Election", 1L, "Election created");
        AuditLog second = auditService.log(
                null, AuditAction.VOTE_CAST, "Election", 1L, "Ballot cast");
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);
        ReflectionTestUtils.setField(second, "details", "altered after the fact");

        ChainVerification verification = auditService.verifyChain();

        assertThat(verification.valid()).isFalse();
        assertThat(verification.firstBrokenId()).isEqualTo(2L);
        assertThat(verification.checkedCount()).isEqualTo(2);
    }

    private void stubAppend() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog record = invocation.getArgument(0);
            stored.add(record);
            return record;
        });
        when(auditLogRepository.findTopByOrderByIdDesc()).thenAnswer(invocation ->
                stored.isEmpty()
                        ? Optional.empty()
                        : Optional.of(stored.get(stored.size() - 1)));
    }

    private void stubReadAll() {
        when(auditLogRepository.findAllByOrderByIdAsc()).thenAnswer(
                invocation -> List.copyOf(stored));
    }
}
