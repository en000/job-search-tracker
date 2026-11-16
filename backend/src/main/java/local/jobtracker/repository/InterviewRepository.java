package local.jobtracker.repository;

import java.util.*;
import local.jobtracker.domain.*;
import org.springframework.data.jpa.repository.*;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {
    @Query("""
        select i from Interview i, Role r
        where i.roleId = r.id and r.workspaceId = :workspaceId and r.id = :roleId and i.id = :id
        """)
    Optional<Interview> findScoped(UUID workspaceId, UUID roleId, UUID id);

    @Query("""
        select i from Interview i, Role r
        where i.roleId = r.id and r.workspaceId = :workspaceId and r.id = :roleId
        order by i.createdAt desc, i.id
        """)
    List<Interview> listScoped(UUID workspaceId, UUID roleId);
}
