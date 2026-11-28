package com.pollpulse.election;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "elections")
public class Election {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ElectionStatus status = ElectionStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "ballot_type", nullable = false, length = 20)
    private BallotType ballotType = BallotType.SINGLE_CHOICE;

    /** Null means every department is eligible. */
    @Column(name = "eligible_department")
    private String eligibleDepartment;

    /** Null means every batch is eligible. */
    @Column(name = "eligible_batch")
    private String eligibleBatch;

    /** Null means every hall is eligible. */
    @Column(name = "eligible_hall")
    private String eligibleHall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Election() {
        // Required by JPA.
    }

    public Election(String title, String description, LocalDateTime startTime, LocalDateTime endTime,
                    User createdBy) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public ElectionStatus getStatus() {
        return status;
    }

    public void setStatus(ElectionStatus status) {
        this.status = status;
    }

    public BallotType getBallotType() {
        return ballotType;
    }

    public void setBallotType(BallotType ballotType) {
        this.ballotType = ballotType;
    }

    public String getEligibleDepartment() {
        return eligibleDepartment;
    }

    public void setEligibleDepartment(String eligibleDepartment) {
        this.eligibleDepartment = eligibleDepartment;
    }

    public String getEligibleBatch() {
        return eligibleBatch;
    }

    public void setEligibleBatch(String eligibleBatch) {
        this.eligibleBatch = eligibleBatch;
    }

    public String getEligibleHall() {
        return eligibleHall;
    }

    public void setEligibleHall(String eligibleHall) {
        this.eligibleHall = eligibleHall;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
