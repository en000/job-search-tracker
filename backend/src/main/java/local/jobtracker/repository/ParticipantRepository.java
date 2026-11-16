package local.jobtracker.repository;

import java.util.*;
import local.jobtracker.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    @Query("""
        select p from Participant p, Interview i, Role r
        where p.interviewId = i.id and i.roleId = r.id
          and r.workspaceId = :workspaceId and r.id = :roleId and i.id = :interviewId
        order by p.position
        """)
    List<Participant> listScoped(UUID workspaceId, UUID roleId, UUID interviewId);
}
