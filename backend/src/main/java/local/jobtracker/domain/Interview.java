package local.jobtracker.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

import static local.jobtracker.domain.Codes.*;

@Entity
@Table(name = "interviews")
public class Interview {
    @Id
    public UUID id;
    public UUID roleId;
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
