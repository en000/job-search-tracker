package local.jobtracker.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

import static local.jobtracker.domain.Codes.*;

@Entity
@Table(name = "workspaces")
public class Workspace {
    @Id
    public UUID id;
    public String timeZone;
    public Instant createdAt;

}
