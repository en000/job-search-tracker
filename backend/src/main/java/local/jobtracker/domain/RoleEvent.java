package local.jobtracker.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import static local.jobtracker.domain.Codes.*;

@Entity
@Table(name = "role_events")
public class RoleEvent {
    @Id
    public UUID id;
    public UUID roleId;
    public UUID interviewId;
    public UUID assignmentId;
    @Enumerated(EnumType.STRING)
    public EventKind kind;
    public Instant occurredAt;
    public Instant createdAt;
    public String body;
    public Map<String,Object> details;

}
