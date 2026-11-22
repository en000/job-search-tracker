package com.jobtracker.services;

import java.time.Instant;
import java.util.*;
import com.jobtracker.domain.Role;
import com.jobtracker.repository.RoleRepository;
import org.springframework.stereotype.Component;

@Component
class RoleWorkflow {
    private final RoleRepository roles;

    RoleWorkflow(RoleRepository roles) {
        this.roles = roles;
    }

    Role read(UUID id) {
        return roles.findById(id)
                .orElseThrow(() -> TrackerException.missing("Role"));
    }

    Role lock(UUID id) {
        return roles.lock(id).orElseThrow(() -> TrackerException.missing("Role"));
    }

    static void requireActive(Role role) {
        if (!role.isActive()) throw TrackerException.conflict("role_completed", "Reopen this role to make that change.");
    }

    static void touch(Role role, Instant now) {
        role.lastActivityAt = now;
    }

    static Map<String, Object> snapshot(Role role) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("status", role.status);
        state.put("stage", role.stage);
        state.put("outcome", role.outcome);
        return state;
    }
}
