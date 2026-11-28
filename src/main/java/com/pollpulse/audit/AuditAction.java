package com.pollpulse.audit;

/**
 * Every kind of event the audit trail can record. Other modules pass one of these
 * values to {@link AuditService#log} when they change something worth remembering.
 */
public enum AuditAction {
    ELECTION_CREATED,
    ELECTION_UPDATED,
    ELECTION_DELETED,
    ELECTION_STATUS_CHANGED,
    POSITION_CREATED,
    POSITION_UPDATED,
    POSITION_DELETED,
    CANDIDATE_APPLIED,
    CANDIDATE_UPDATED,
    CANDIDATE_WITHDRAWN,
    CANDIDATE_REVIEWED,
    VOTER_IMPORTED,
    VOTER_AUTO_REGISTERED,
    BALLOT_TOKEN_ISSUED,
    VOTE_CAST,
    VOTE_CHAIN_VERIFIED,
    RESULTS_PUBLISHED
}
