package local.jobtracker.repository;

import java.util.*;
import local.jobtracker.domain.*;
import org.springframework.data.jpa.repository.*;

public interface EventRepository extends JpaRepository<RoleEvent, UUID> {
    String TIMELINE = """
        select e from RoleEvent e, Role r
        where e.roleId = r.id and r.workspaceId = :workspaceId and r.id = :roleId
        order by e.occurredAt desc, e.createdAt desc, e.id
        """;

    @Query(TIMELINE)
    org.springframework.data.domain.Page<RoleEvent> pageScoped(
        UUID workspaceId, UUID roleId, org.springframework.data.domain.Pageable page);

    @Query(TIMELINE)
    List<RoleEvent> listScoped(UUID workspaceId, UUID roleId);

    @Query("""
        select e from RoleEvent e, Role r
        where e.roleId = r.id and r.workspaceId = :workspaceId and r.id = :roleId and e.id = :id
        """)
    Optional<RoleEvent> findScoped(UUID workspaceId, UUID roleId, UUID id);
}
