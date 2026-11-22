package com.jobtracker.repository;

import java.util.*;
import com.jobtracker.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    @Query("""
        select p from Participant p
        where p.interview.role.id = :roleId and p.interview.id = :interviewId
        order by p.position
        """)
    List<Participant> listScoped(UUID roleId, UUID interviewId);
}
