package com.jobtracker.domain;

import com.jobtracker.domain.enums.AssignmentStatus;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;


@Entity
@Table(name = "assignments")
public class Assignment {
    @Id
    public UUID id;

    public UUID getId() {
        return id;
    }
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    public Role role;
    public String title;
    @Enumerated(EnumType.STRING)
    public AssignmentStatus status;
    public LocalDate receivedOn;
    public LocalDate dueOn;
    public String instructions;
    public String instructionsUrl;
    public String workUrl;
    public String notes;
    public String feedback;
    public Instant submittedAt;
    @Version
    public Long version;
    public Instant createdAt;
    public Instant updatedAt;

    public boolean requiresSubmissionTime() {
        return switch (status) {
            case SUBMITTED, PASSED, DID_NOT_PASS -> true;
            default -> false;
        };
    }

}
