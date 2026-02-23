package com.complaints.controller;

import com.complaints.dto.Dtos;
import com.complaints.entity.User;
import com.complaints.exception.ApiException;
import com.complaints.repository.UserRepository;
import com.complaints.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Dtos.UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(AuthService::mapToUserDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public List<Dtos.UserDto> getAgents() {
        return userRepository.findByRoleAndActive(User.Role.AGENT, true).stream()
                .map(AuthService::mapToUserDto)
                .collect(Collectors.toList());
    }

    @PatchMapping("/me")
    public Dtos.UserDto updateProfile(@RequestBody Dtos.UpdateUserRequest request,
                                       @AuthenticationPrincipal User currentUser) {
        if (request.getName() != null) currentUser.setName(request.getName());
        if (request.getPhone() != null) currentUser.setPhone(request.getPhone());
        if (request.getDepartment() != null) currentUser.setDepartment(request.getDepartment());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            currentUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return AuthService.mapToUserDto(userRepository.save(currentUser));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public Dtos.UserDto toggleActive(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        user.setActive(!user.isActive());
        return AuthService.mapToUserDto(userRepository.save(user));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public Dtos.UserDto changeRole(@PathVariable Long id, @RequestParam User.Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        user.setRole(role);
        return AuthService.mapToUserDto(userRepository.save(user));
    }
}
