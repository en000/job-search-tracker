package com.jobtracker.domain;

import com.jobtracker.domain.enums.EventKind;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "role_events")
public class RoleEvent {
    @Id
    public UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    public Role role;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    public Interview interview;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    public Assignment assignment;
    @Enumerated(EnumType.STRING)
    public EventKind kind;
    public Instant occurredAt;
    public Instant createdAt;
    public String body;
    public Map<String,Object> details;

}
