package local.jobtracker.controllers;

import jakarta.validation.Valid;
import java.util.Map;
import local.jobtracker.dtos.Requests.SettingsInput;
import local.jobtracker.dtos.Responses.*;
import local.jobtracker.services.WorkspaceService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class WorkspaceController {
    private final WorkspaceService workspace;

    WorkspaceController(WorkspaceService workspace) {
        this.workspace = workspace;
    }

    @GetMapping("/health")
    Map<String, String> health() {
        workspace.settings();
        return Map.of("status", "UP");
    }

    @GetMapping("/today")
    Today today() {
        return workspace.today();
    }

    @GetMapping("/settings")
    Settings settings() {
        return workspace.settings();
    }

    @PutMapping("/settings")
    Settings updateSettings(@Valid @RequestBody SettingsInput request) {
        return workspace.updateZone(request.timeZone());
    }

    @GetMapping("/export")
    ResponseEntity<Export> export() {
        Export snapshot = workspace.export();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=job-search-tracker.json")
                .body(snapshot);
    }
}
