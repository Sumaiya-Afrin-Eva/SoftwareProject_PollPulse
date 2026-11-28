package com.pollpulse.audit;

/**
 * Result of walking the audit chain.
 *
 * @param valid         true when every record links to its predecessor and its hash matches
 * @param firstBrokenId id of the first record that fails verification, or null when the chain is intact
 * @param checkedCount  how many records were inspected before the walk finished or stopped
 */
public record ChainVerification(boolean valid, Long firstBrokenId, long checkedCount) {
}
