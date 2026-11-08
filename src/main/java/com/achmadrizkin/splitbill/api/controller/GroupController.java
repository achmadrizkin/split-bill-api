package com.achmadrizkin.splitbill.api.controller;

import com.achmadrizkin.splitbill.api.dto.Requests.AddParticipantRequest;
import com.achmadrizkin.splitbill.api.dto.Requests.CreateGroupRequest;
import com.achmadrizkin.splitbill.api.dto.Responses.AuditEventResponse;
import com.achmadrizkin.splitbill.api.dto.Responses.GroupResponse;
import com.achmadrizkin.splitbill.api.dto.Responses.ParticipantResponse;
import com.achmadrizkin.splitbill.application.AuditService;
import com.achmadrizkin.splitbill.application.GroupService;
import com.achmadrizkin.splitbill.application.view.Views.GroupView;
import com.achmadrizkin.splitbill.application.view.Views.ParticipantView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups", description = "Bill groups and their participants")
public class GroupController {

    private final GroupService groups;
    private final AuditService audit;

    public GroupController(GroupService groups, AuditService audit) {
        this.groups = groups;
        this.audit = audit;
    }

    @PostMapping
    @Operation(summary = "Create a bill group with its participants")
    public ResponseEntity<GroupResponse> create(@Valid @RequestBody CreateGroupRequest request) {
        GroupView group = groups.create(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(group.id()).toUri();
        return ResponseEntity.created(location).body(GroupResponse.from(group));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get a bill group")
    public GroupResponse get(@PathVariable UUID groupId) {
        return GroupResponse.from(groups.get(groupId));
    }

    @PostMapping("/{groupId}/participants")
    @Operation(summary = "Add a participant to an existing group")
    public ResponseEntity<ParticipantResponse> addParticipant(@PathVariable UUID groupId,
                                                              @Valid @RequestBody AddParticipantRequest request) {
        ParticipantView participant = groups.addParticipant(groupId, request.name());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/groups/{groupId}").buildAndExpand(groupId).toUri();
        return ResponseEntity.created(location).body(ParticipantResponse.from(participant));
    }

    @GetMapping("/{groupId}/audit-events")
    @Operation(summary = "Audit trail of every change made to the group")
    public List<AuditEventResponse> auditEvents(@PathVariable UUID groupId) {
        return audit.list(groupId).stream().map(AuditEventResponse::from).toList();
    }
}
