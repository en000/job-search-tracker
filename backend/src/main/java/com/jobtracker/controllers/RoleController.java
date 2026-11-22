package com.jobtracker.controllers;

import com.jobtracker.dtos.responses.EventDetails;
import com.jobtracker.dtos.responses.Page;
import com.jobtracker.dtos.responses.RoleDetails;
import com.jobtracker.dtos.responses.RoleSummary;

import com.jobtracker.domain.enums.RoleStatus;
import com.jobtracker.domain.enums.Stage;
import com.jobtracker.domain.enums.Outcome;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.*;
import com.jobtracker.dtos.requests.*;
import com.jobtracker.services.RoleService;
import com.jobtracker.services.TrackerException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    Page<RoleSummary> list(@RequestParam(required = false) @Size(max = 200) String q,
            @RequestParam(required = false) RoleStatus status, @RequestParam(required = false) Stage stage,
            @RequestParam(required = false) Outcome outcome, @RequestParam(defaultValue = "recent") RoleSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return roles.list(q, status, stage, outcome, sort, page, size);
    }

    @PostMapping
    ResponseEntity<RoleDetails> create(@Valid @RequestBody RoleCreate request) {
        var role = roles.create(request);
        return ResponseEntity.created(URI.create("/api/roles/" + role.id())).body(role);
    }

    @GetMapping("/{roleId}")
    RoleDetails get(@PathVariable UUID roleId) {
        return roles.get(roleId);
    }

    @PatchMapping("/{roleId}")
    RoleDetails patch(@PathVariable UUID roleId, @RequestBody JsonNode body) throws JsonProcessingException {
        if (!body.isObject()) throw TrackerException.invalid("body", "Send a JSON object.");
        RolePatch request = json.treeToValue(body, RolePatch.class);
        var errors = validator.validate(request);
        if (!errors.isEmpty()) throw new jakarta.validation.ConstraintViolationException(errors);
        Set<String> fields = new HashSet<>();
        body.fieldNames().forEachRemaining(fields::add);
        return roles.patch(roleId, request, fields);
    }

    @PostMapping("/{roleId}/complete")
    RoleDetails complete(@PathVariable UUID roleId, @Valid @RequestBody Completion request) {
        return roles.complete(roleId, request);
    }

    @PostMapping("/{roleId}/reopen")
    RoleDetails reopen(@PathVariable UUID roleId, @Valid @RequestBody Version request) {
        return roles.reopen(roleId, request);
    }

    @PostMapping("/{roleId}/completion-suggestion")
    RoleDetails changeCompletionSuggestion(@PathVariable UUID roleId,
            @Valid @RequestBody CompletionSuggestionChange request) {
        return roles.changeCompletionSuggestion(roleId, request);
    }

    @PutMapping("/{roleId}/follow-up")
    RoleDetails setFollowUp(@PathVariable UUID roleId, @Valid @RequestBody FollowUpChange request) {
        return roles.setFollowUp(roleId, request);
    }

    @PostMapping("/{roleId}/follow-up/complete")
    RoleDetails completeFollowUp(@PathVariable UUID roleId, @Valid @RequestBody Version request) {
        return roles.completeFollowUp(roleId, request);
    }

}
