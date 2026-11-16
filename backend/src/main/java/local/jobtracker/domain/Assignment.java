package local.jobtracker.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

import static local.jobtracker.domain.Codes.*;

@Entity
@Table(name = "assignments")
public class Assignment {
    @Id
    public UUID id;
    public UUID roleId;
    public String title;
    @Enumerated(EnumType.STRING)
    public AssignmentStatus status;
    public LocalDate receivedOn;
    public LocalDate dueOn;
    public String brief;
    public String briefUrl;
    public String workUrl;
    public String submissionUrl;
    public String workNotes;
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
