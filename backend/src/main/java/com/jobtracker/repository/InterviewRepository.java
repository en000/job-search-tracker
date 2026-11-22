package com.jobtracker.repository;

import java.util.*;
import com.jobtracker.domain.*;
import org.springframework.data.jpa.repository.*;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {
    @Query("""
        select i from Interview i
        where i.role.id = :roleId and i.id = :id
        """)
    Optional<Interview> findScoped(UUID roleId, UUID id);

    @Query("""
        select i from Interview i
        where i.role.id = :roleId
        order by i.createdAt desc, i.id
        """)
    List<Interview> listScoped(UUID roleId);
}
