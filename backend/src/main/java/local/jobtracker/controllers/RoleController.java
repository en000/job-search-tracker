package local.jobtracker.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.*;
import local.jobtracker.dtos.Requests.*;
import local.jobtracker.dtos.Responses;
import local.jobtracker.services.RoleService;
import local.jobtracker.services.TrackerException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static local.jobtracker.domain.Codes.*;

@RestController
@RequestMapping("/api/roles")
class RoleController {
    private final RoleService roles;
    private final ObjectMapper json;
    private final Validator validator;

    RoleController(RoleService roles, ObjectMapper json, Validator validator) {
        this.roles = roles;
        this.json = json;
        this.validator = validator;
    }

    @GetMapping
    Responses.Page<Responses.RoleSummary> list(@RequestParam(required = false) @Size(max = 200) String q,
            @RequestParam(required = false) Stage stage, @RequestParam(required = false) Substage substage,
            @RequestParam(required = false) Outcome outcome, @RequestParam(defaultValue = "recent") RoleSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return roles.list(q, stage, substage, outcome, sort, page, size);
    }

    @PostMapping
    ResponseEntity<Responses.RoleDetails> create(@Valid @RequestBody RoleCreate request) {
        var role = roles.create(request);
        return ResponseEntity.created(URI.create("/api/roles/" + role.id())).body(role);
    }

    @GetMapping("/{roleId}")
    Responses.RoleDetails get(@PathVariable UUID roleId) {
        return roles.get(roleId);
    }

    @PatchMapping("/{roleId}")
    Responses.RoleDetails patch(@PathVariable UUID roleId, @RequestBody JsonNode body) throws JsonProcessingException {
        if (!body.isObject()) throw TrackerException.invalid("body", "Send a JSON object.");
        RolePatch request = json.treeToValue(body, RolePatch.class);
        var errors = validator.validate(request);
        if (!errors.isEmpty()) throw new jakarta.validation.ConstraintViolationException(errors);
        Set<String> fields = new HashSet<>();
        body.fieldNames().forEachRemaining(fields::add);
        return roles.patch(roleId, request, fields);
    }

    @PostMapping("/{roleId}/complete")
    Responses.RoleDetails complete(@PathVariable UUID roleId, @Valid @RequestBody Completion request) {
        return roles.complete(roleId, request);
    }

    @PostMapping("/{roleId}/reopen")
    Responses.RoleDetails reopen(@PathVariable UUID roleId, @Valid @RequestBody Version request) {
        return roles.reopen(roleId, request);
    }

    @PostMapping("/{roleId}/completion-suggestion")
    Responses.RoleDetails changeCompletionSuggestion(@PathVariable UUID roleId,
            @Valid @RequestBody CompletionSuggestionChange request) {
        return roles.changeCompletionSuggestion(roleId, request);
    }

    @PutMapping("/{roleId}/follow-up")
    Responses.RoleDetails setFollowUp(@PathVariable UUID roleId, @Valid @RequestBody FollowUpChange request) {
        return roles.setFollowUp(roleId, request);
    }

    @PostMapping("/{roleId}/follow-up/complete")
    Responses.RoleDetails completeFollowUp(@PathVariable UUID roleId, @Valid @RequestBody Version request) {
        return roles.completeFollowUp(roleId, request);
    }

    @GetMapping("/{roleId}/timeline")
    Responses.Page<Responses.EventDetails> timeline(@PathVariable UUID roleId,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return roles.timeline(roleId, page, size);
    }

    @GetMapping("/{roleId}/timeline/{eventId}")
    Responses.EventDetails timelineEvent(@PathVariable UUID roleId, @PathVariable UUID eventId) {
        return roles.timelineEvent(roleId, eventId);
    }

    @PostMapping("/{roleId}/timeline")
    ResponseEntity<Responses.EventDetails> log(@PathVariable UUID roleId, @Valid @RequestBody EventInput request) {
        var event = roles.log(roleId, request);
        return ResponseEntity.created(URI.create("/api/roles/" + roleId + "/timeline/" + event.id())).body(event);
    }
}
