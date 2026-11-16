package local.jobtracker.repository;

import java.util.*;
import local.jobtracker.domain.*;
import org.springframework.data.jpa.repository.*;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByWorkspaceIdAndId(UUID workspaceId, UUID id);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Role r where r.workspaceId = :workspaceId and r.id = :id")
    Optional<Role> lock(UUID workspaceId, UUID id);

    List<Role> findByWorkspaceIdOrderByLastActivityAtDescIdAsc(UUID workspaceId);
}
