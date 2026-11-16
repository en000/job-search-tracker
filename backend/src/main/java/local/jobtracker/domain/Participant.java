package local.jobtracker.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

import static local.jobtracker.domain.Codes.*;

@Entity
@Table(name = "interview_participants")
public class Participant {
    @Id
    public UUID id;
    public UUID interviewId;
    public String name;
    public String jobTitle;
    public String email;
    public int position;

}
