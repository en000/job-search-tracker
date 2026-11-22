package com.jobtracker.repository;

import java.util.*;
import com.jobtracker.domain.*;
import org.springframework.data.jpa.repository.*;

public interface RoleEventRepository extends JpaRepository<RoleEvent, UUID> {
    String TIMELINE = """
        select e from RoleEvent e
        where e.role.id = :roleId
        order by e.occurredAt desc, e.createdAt desc, e.id
        """;

    @Query(TIMELINE)
    org.springframework.data.domain.Page<RoleEvent> pageScoped(
        UUID roleId, org.springframework.data.domain.Pageable page);

    @Query(TIMELINE)
    List<RoleEvent> listScoped(UUID roleId);

    @Query("""
        select e from RoleEvent e
        where e.role.id = :roleId and e.id = :id
        """)
    Optional<RoleEvent> findScoped(UUID roleId, UUID id);
}
