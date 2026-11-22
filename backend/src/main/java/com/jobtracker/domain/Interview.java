package com.jobtracker.domain;

import com.jobtracker.domain.enums.InterviewType;
import com.jobtracker.domain.enums.InterviewStatus;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;


@Entity
@Table(name = "interviews")
public class Interview {
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
    public InterviewType type;
    @Enumerated(EnumType.STRING)
    public InterviewStatus status;
    public Integer roundNumber;
    public Instant startsAt;
    public Integer durationMinutes;
    public String meetingUrl;
    public String location;
    public String prepNotes;
    public String interviewNotes;
    public String feedback;
    @Version
    public Long version;
    public Instant createdAt;
    public Instant updatedAt;

}
