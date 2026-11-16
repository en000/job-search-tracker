package local.jobtracker.controllers;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import local.jobtracker.dtos.Requests.*;
import local.jobtracker.dtos.Responses.*;
import local.jobtracker.services.ChildService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles/{roleId}")
class ChildController {
    private final ChildService children;

    ChildController(ChildService children) {
        this.children = children;
    }

    @GetMapping("/interviews")
    List<InterviewDetails> interviews(@PathVariable UUID roleId) {
        return children.listInterviews(roleId);
    }

    @PostMapping("/interviews")
    ResponseEntity<InterviewDetails> createInterview(@PathVariable UUID roleId, @Valid @RequestBody InterviewInput input) {
        var interview = children.createInterview(roleId, input);
        return ResponseEntity.created(URI.create("/api/roles/" + roleId + "/interviews/" + interview.id())).body(interview);
    }

    @GetMapping("/interviews/{interviewId}")
    InterviewDetails interview(@PathVariable UUID roleId, @PathVariable UUID interviewId) {
        return children.getInterview(roleId, interviewId);
    }

    @PutMapping("/interviews/{interviewId}")
    InterviewDetails updateInterview(@PathVariable UUID roleId, @PathVariable UUID interviewId,
                                     @Valid @RequestBody InterviewInput input) {
        return children.updateInterview(roleId, interviewId, input);
    }

    @GetMapping("/assignments")
    List<AssignmentDetails> assignments(@PathVariable UUID roleId) {
        return children.listAssignments(roleId);
    }

    @PostMapping("/assignments")
    ResponseEntity<AssignmentDetails> createAssignment(@PathVariable UUID roleId, @Valid @RequestBody AssignmentInput input) {
        var assignment = children.createAssignment(roleId, input);
        return ResponseEntity.created(URI.create("/api/roles/" + roleId + "/assignments/" + assignment.id())).body(assignment);
    }

    @GetMapping("/assignments/{assignmentId}")
    AssignmentDetails assignment(@PathVariable UUID roleId, @PathVariable UUID assignmentId) {
        return children.getAssignment(roleId, assignmentId);
    }

    @PutMapping("/assignments/{assignmentId}")
    AssignmentDetails updateAssignment(@PathVariable UUID roleId, @PathVariable UUID assignmentId,
                                        @Valid @RequestBody AssignmentInput input) {
        return children.updateAssignment(roleId, assignmentId, input);
    }
}
