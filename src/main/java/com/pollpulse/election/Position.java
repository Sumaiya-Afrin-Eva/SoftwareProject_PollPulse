package com.pollpulse.election;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "positions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_positions_election_title",
                columnNames = {"election_id", "title"}))
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "max_winners", nullable = false)
    private int maxWinners = 1;

    protected Position() {
        // Required by JPA.
    }

    public Position(Election election, String title, int maxWinners) {
        this.election = election;
        this.title = title;
        this.maxWinners = maxWinners;
    }

    public Long getId() {
        return id;
    }

    public Election getElection() {
        return election;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getMaxWinners() {
        return maxWinners;
    }

    public void setMaxWinners(int maxWinners) {
        this.maxWinners = maxWinners;
    }
}
