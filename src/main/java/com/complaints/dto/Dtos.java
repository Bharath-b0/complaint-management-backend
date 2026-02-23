package com.complaints.dto;

import com.complaints.entity.Complaint;
import com.complaints.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class Dtos {

    // ================== AUTH ==================

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank
        private String name;
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
        private String phone;
        private String department;
        private User.Role role = User.Role.USER;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private UserDto user;
    }

    // ================== USER ==================

    @Data
    public static class UserDto {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String department;
        private User.Role role;
        private boolean active;
        private LocalDateTime createdAt;
    }

    @Data
    public static class UpdateUserRequest {
        private String name;
        private String phone;
        private String department;
        private String password;
    }

    // ================== COMPLAINT ==================

    @Data
    public static class CreateComplaintRequest {
        @NotBlank
        private String title;
        @NotBlank
        private String description;
        @NotNull
        private Complaint.Category category;
        @NotNull
        private Complaint.Priority priority;
        private LocalDateTime dueDate;
    }

    @Data
    public static class UpdateComplaintRequest {
        private String title;
        private String description;
        private Complaint.Category category;
        private Complaint.Priority priority;
        private Complaint.Status status;
        private Long assignedToId;
        private String resolution;
        private LocalDateTime dueDate;
    }

    @Data
    public static class ComplaintDto {
        private Long id;
        private String ticketNumber;
        private String title;
        private String description;
        private Complaint.Category category;
        private Complaint.Status status;
        private Complaint.Priority priority;
        private UserDto submittedBy;
        private UserDto assignedTo;
        private String resolution;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime resolvedAt;
        private LocalDateTime dueDate;
        private int commentCount;
        private int attachmentCount;
    }

    @Data
    public static class ComplaintDetailDto extends ComplaintDto {
        private List<CommentDto> comments;
        private List<AttachmentDto> attachments;
    }

    // ================== COMMENT ==================

    @Data
    public static class CreateCommentRequest {
        @NotBlank
        private String content;
        private boolean internal = false;
    }

    @Data
    public static class CommentDto {
        private Long id;
        private String content;
        private UserDto author;
        private boolean internal;
        private LocalDateTime createdAt;
    }

    // ================== ATTACHMENT ==================

    @Data
    public static class AttachmentDto {
        private Long id;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private LocalDateTime createdAt;
    }

    // ================== ANALYTICS ==================

    @Data
    public static class DashboardStats {
        private long totalComplaints;
        private long openComplaints;
        private long inProgressComplaints;
        private long resolvedComplaints;
        private long closedComplaints;
        private long criticalComplaints;
        private long overdueComplaints;
        private long newThisWeek;
        private List<CategoryCount> byCategory;
        private List<StatusCount> byStatus;
    }

    @Data
    public static class CategoryCount {
        private String category;
        private Long count;
    }

    @Data
    public static class StatusCount {
        private String status;
        private Long count;
    }

    // ================== PAGINATION ==================

    @Data
    public static class PageResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}
