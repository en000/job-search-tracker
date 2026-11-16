package local.jobtracker.repository;

import java.util.*;
import local.jobtracker.domain.*;
import org.springframework.data.jpa.repository.*;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    @Query("""
        select a from Assignment a, Role r
        where a.roleId = r.id and r.workspaceId = :workspaceId and r.id = :roleId and a.id = :id
        """)
    Optional<Assignment> findScoped(UUID workspaceId, UUID roleId, UUID id);

    @Query("""
        select a from Assignment a, Role r
        where a.roleId = r.id and r.workspaceId = :workspaceId and r.id = :roleId
        order by a.createdAt desc, a.id
        """)
    List<Assignment> listScoped(UUID workspaceId, UUID roleId);
}
