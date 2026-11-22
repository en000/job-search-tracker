package com.jobtracker.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;


@Entity
@Table(name = "interview_participants")
public class Participant {
    @Id
    public UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false)
    public Interview interview;
    public String name;
    public String jobTitle;
    public String email;
    public int position;

}
