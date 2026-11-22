package com.jobtracker.repository;

import java.util.*;
import com.jobtracker.domain.*;
import org.springframework.data.jpa.repository.*;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    @Query("""
        select a from Assignment a
        where a.role.id = :roleId and a.id = :id
        """)
    Optional<Assignment> findScoped(UUID roleId, UUID id);

    @Query("""
        select a from Assignment a
        where a.role.id = :roleId
        order by a.createdAt desc, a.id
        """)
    List<Assignment> listScoped(UUID roleId);
}
