package local.jobtracker.services;

import java.util.UUID;
import local.jobtracker.domain.Workspace;
import local.jobtracker.repository.WorkspaceRepository;
import org.springframework.stereotype.Component;

@Component
class WorkspaceContext {
    private static final UUID LOCAL_ID = UUID.fromString("a1d86a7c-6c20-462e-8d2e-4fcee925a10a");
    private final WorkspaceRepository workspaces;

    WorkspaceContext(WorkspaceRepository workspaces) {
        this.workspaces = workspaces;
    }

    UUID id() {
        return LOCAL_ID;
    }

    Workspace current() {
        return workspaces.findById(id()).orElseThrow(() -> TrackerException.missing("Workspace"));
    }
}
