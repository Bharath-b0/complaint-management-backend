package com.complaints.controller;

import com.complaints.dto.Dtos;
import com.complaints.entity.User;
import com.complaints.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    public ResponseEntity<Dtos.ComplaintDto> create(
            @Valid @RequestBody Dtos.CreateComplaintRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complaintService.create(request, user));
    }

    @GetMapping
    public ResponseEntity<Dtos.PageResponse<Dtos.ComplaintDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(complaintService.getAll(page, size, status, priority, category, search, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dtos.ComplaintDetailDto> getById(@PathVariable Long id,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(complaintService.getById(id, user));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Dtos.ComplaintDto> update(@PathVariable Long id,
                                                     @RequestBody Dtos.UpdateComplaintRequest request,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(complaintService.update(id, request, user));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Dtos.CommentDto> addComment(@PathVariable Long id,
                                                       @Valid @RequestBody Dtos.CreateCommentRequest request,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complaintService.addComment(id, request, user));
    }

    @GetMapping("/stats/dashboard")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<Dtos.DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(complaintService.getDashboardStats());
    }
}
