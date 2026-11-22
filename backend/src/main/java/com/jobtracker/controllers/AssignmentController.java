package com.jobtracker.controllers;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import com.jobtracker.dtos.requests.*;
import com.jobtracker.dtos.responses.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.jobtracker.services.AssignmentService;

@RestController
@RequestMapping("/api/roles/{roleId}/assignments")
class AssignmentController {
    private final AssignmentService assignments;

    AssignmentController(AssignmentService assignments) {
        this.assignments = assignments;
    }

    @GetMapping
    List<AssignmentDetails> assignments(@PathVariable UUID roleId) {
        return assignments.listAssignments(roleId);
    }

    @PostMapping
    ResponseEntity<AssignmentDetails> createAssignment(@PathVariable UUID roleId, @Valid @RequestBody AssignmentInput input) {
        var assignment = assignments.createAssignment(roleId, input);
        return ResponseEntity.created(URI.create("/api/roles/" + roleId + "/assignments/" + assignment.id())).body(assignment);
    }

    @GetMapping("/{assignmentId}")
    AssignmentDetails assignment(@PathVariable UUID roleId, @PathVariable UUID assignmentId) {
        return assignments.getAssignment(roleId, assignmentId);
    }

    @PutMapping("/{assignmentId}")
    AssignmentDetails updateAssignment(@PathVariable UUID roleId, @PathVariable UUID assignmentId,
                                        @Valid @RequestBody AssignmentInput input) {
        return assignments.updateAssignment(roleId, assignmentId, input);
    }
}
