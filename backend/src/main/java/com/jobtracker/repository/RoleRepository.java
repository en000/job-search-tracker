package com.jobtracker.repository;

import java.util.*;
import com.jobtracker.domain.*;
import org.springframework.data.jpa.repository.*;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Role r where r.id = :id")
    Optional<Role> lock(UUID id);

    List<Role> findAllByOrderByLastActivityAtDescIdAsc();
}
