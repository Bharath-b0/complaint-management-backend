package com.complaints.service;

import com.complaints.dto.Dtos;
import com.complaints.entity.*;
import com.complaints.exception.ApiException;
import com.complaints.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public Dtos.ComplaintDto create(Dtos.CreateComplaintRequest request, User currentUser) {
        String ticketNumber = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Complaint complaint = Complaint.builder()
                .ticketNumber(ticketNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(request.getPriority())
                .submittedBy(currentUser)
                .dueDate(request.getDueDate())
                .build();

        return mapToDto(complaintRepository.save(complaint));
    }

    public Dtos.PageResponse<Dtos.ComplaintDto> getAll(int page, int size, String status,
                                                         String priority, String category,
                                                         String search, User currentUser) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Complaint> complaintPage;

        if (search != null && !search.isBlank()) {
            complaintPage = complaintRepository.searchComplaints(search, pageable);
        } else if (status != null) {
            Complaint.Status s = Complaint.Status.valueOf(status.toUpperCase());
            complaintPage = complaintRepository.findByStatus(s, pageable);
        } else if (currentUser.getRole() == User.Role.USER) {
            complaintPage = complaintRepository.findBySubmittedBy(currentUser, pageable);
        } else if (currentUser.getRole() == User.Role.AGENT) {
            complaintPage = complaintRepository.findByAssignedTo(currentUser, pageable);
        } else {
            complaintPage = complaintRepository.findAll(pageable);
        }

        return buildPageResponse(complaintPage, page, size);
    }

    public Dtos.ComplaintDetailDto getById(Long id, User currentUser) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ApiException("Complaint not found", HttpStatus.NOT_FOUND));

        checkAccess(complaint, currentUser);

        Dtos.ComplaintDetailDto dto = new Dtos.ComplaintDetailDto();
        copyComplaintToDto(complaint, dto);

        boolean isStaff = currentUser.getRole() != User.Role.USER;

        List<Comment> comments = isStaff
                ? commentRepository.findByComplaintOrderByCreatedAtAsc(complaint)
                : commentRepository.findByComplaintAndInternalFalseOrderByCreatedAtAsc(complaint);

        dto.setComments(comments.stream().map(this::mapCommentToDto).collect(Collectors.toList()));
        dto.setAttachments(
            complaint.getAttachments() != null
                ? complaint.getAttachments().stream().map(this::mapAttachmentToDto).collect(Collectors.toList())
                : List.of()
        );
        return dto;
    }

    @Transactional
    public Dtos.ComplaintDto update(Long id, Dtos.UpdateComplaintRequest request, User currentUser) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ApiException("Complaint not found", HttpStatus.NOT_FOUND));

        checkModifyAccess(complaint, currentUser);

        if (request.getTitle() != null) complaint.setTitle(request.getTitle());
        if (request.getDescription() != null) complaint.setDescription(request.getDescription());
        if (request.getCategory() != null) complaint.setCategory(request.getCategory());
        if (request.getPriority() != null) complaint.setPriority(request.getPriority());
        if (request.getDueDate() != null) complaint.setDueDate(request.getDueDate());
        if (request.getResolution() != null) complaint.setResolution(request.getResolution());

        if (request.getStatus() != null) {
            complaint.setStatus(request.getStatus());
            if (request.getStatus() == Complaint.Status.RESOLVED || request.getStatus() == Complaint.Status.CLOSED) {
                complaint.setResolvedAt(LocalDateTime.now());
            }
        }

        if (request.getAssignedToId() != null) {
            User agent = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new ApiException("Agent not found", HttpStatus.NOT_FOUND));
            complaint.setAssignedTo(agent);
            if (complaint.getStatus() == Complaint.Status.OPEN) {
                complaint.setStatus(Complaint.Status.IN_PROGRESS);
            }
        }

        return mapToDto(complaintRepository.save(complaint));
    }

    @Transactional
    public Dtos.CommentDto addComment(Long complaintId, Dtos.CreateCommentRequest request, User currentUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException("Complaint not found", HttpStatus.NOT_FOUND));

        checkAccess(complaint, currentUser);

        boolean canPostInternal = currentUser.getRole() != User.Role.USER;

        Comment comment = Comment.builder()
                .complaint(complaint)
                .author(currentUser)
                .content(request.getContent())
                .internal(request.isInternal() && canPostInternal)
                .build();

        return mapCommentToDto(commentRepository.save(comment));
    }

    public Dtos.DashboardStats getDashboardStats() {
        Dtos.DashboardStats stats = new Dtos.DashboardStats();
        stats.setTotalComplaints(complaintRepository.count());
        stats.setOpenComplaints(complaintRepository.countByStatus(Complaint.Status.OPEN));
        stats.setInProgressComplaints(complaintRepository.countByStatus(Complaint.Status.IN_PROGRESS));
        stats.setResolvedComplaints(complaintRepository.countByStatus(Complaint.Status.RESOLVED));
        stats.setClosedComplaints(complaintRepository.countByStatus(Complaint.Status.CLOSED));
        stats.setCriticalComplaints(complaintRepository.countByPriority(Complaint.Priority.CRITICAL));
        stats.setOverdueComplaints(complaintRepository.findOverdueComplaints(LocalDateTime.now()).size());

        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        stats.setNewThisWeek(complaintRepository.countByDateRange(weekAgo, LocalDateTime.now()));

        List<Dtos.CategoryCount> catCounts = complaintRepository.countByCategory().stream()
                .map(row -> {
                    Dtos.CategoryCount c = new Dtos.CategoryCount();
                    c.setCategory(row[0].toString());
                    c.setCount((Long) row[1]);
                    return c;
                }).collect(Collectors.toList());
        stats.setByCategory(catCounts);

        List<Dtos.StatusCount> statusCounts = complaintRepository.countByStatusGrouped().stream()
                .map(row -> {
                    Dtos.StatusCount s = new Dtos.StatusCount();
                    s.setStatus(row[0].toString());
                    s.setCount((Long) row[1]);
                    return s;
                }).collect(Collectors.toList());
        stats.setByStatus(statusCounts);

        return stats;
    }

    private void checkAccess(Complaint complaint, User user) {
        if (user.getRole() == User.Role.USER && !complaint.getSubmittedBy().getId().equals(user.getId())) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
    }

    private void checkModifyAccess(Complaint complaint, User user) {
        if (user.getRole() == User.Role.USER) {
            if (!complaint.getSubmittedBy().getId().equals(user.getId())) {
                throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
            }
            if (complaint.getStatus() != Complaint.Status.OPEN) {
                throw new ApiException("Cannot modify complaint in current status", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private Dtos.PageResponse<Dtos.ComplaintDto> buildPageResponse(Page<Complaint> page, int pageNum, int size) {
        Dtos.PageResponse<Dtos.ComplaintDto> response = new Dtos.PageResponse<>();
        response.setContent(page.getContent().stream().map(this::mapToDto).collect(Collectors.toList()));
        response.setPage(pageNum);
        response.setSize(size);
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLast(page.isLast());
        return response;
    }

    private Dtos.ComplaintDto mapToDto(Complaint c) {
        Dtos.ComplaintDto dto = new Dtos.ComplaintDto();
        copyComplaintToDto(c, dto);
        return dto;
    }

    private void copyComplaintToDto(Complaint c, Dtos.ComplaintDto dto) {
        dto.setId(c.getId());
        dto.setTicketNumber(c.getTicketNumber());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setCategory(c.getCategory());
        dto.setStatus(c.getStatus());
        dto.setPriority(c.getPriority());
        dto.setSubmittedBy(AuthService.mapToUserDto(c.getSubmittedBy()));
        dto.setAssignedTo(AuthService.mapToUserDto(c.getAssignedTo()));
        dto.setResolution(c.getResolution());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        dto.setResolvedAt(c.getResolvedAt());
        dto.setDueDate(c.getDueDate());
        dto.setCommentCount(c.getComments() != null ? c.getComments().size() : 0);
        dto.setAttachmentCount(c.getAttachments() != null ? c.getAttachments().size() : 0);
    }

    private Dtos.CommentDto mapCommentToDto(Comment c) {
        Dtos.CommentDto dto = new Dtos.CommentDto();
        dto.setId(c.getId());
        dto.setContent(c.getContent());
        dto.setAuthor(AuthService.mapToUserDto(c.getAuthor()));
        dto.setInternal(c.isInternal());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }

    private Dtos.AttachmentDto mapAttachmentToDto(Attachment a) {
        Dtos.AttachmentDto dto = new Dtos.AttachmentDto();
        dto.setId(a.getId());
        dto.setFileName(a.getFileName());
        dto.setFileType(a.getFileType());
        dto.setFileSize(a.getFileSize());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }
}
