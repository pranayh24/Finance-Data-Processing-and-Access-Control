package com.finance.dashboard.service;

import com.finance.dashboard.dto.RegisterRequest;
import com.finance.dashboard.dto.UpdateUserRoleRequest;
import com.finance.dashboard.dto.UpdateUserStatusRequest;
import com.finance.dashboard.dto.UserResponse;
import com.finance.dashboard.entity.AuditLog;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.exception.ConflictException;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.repository.AuditLogRepository;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository    userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder   passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public UserResponse createUser(RegisterRequest request, UUID actorId) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        user = userRepository.save(user);
        audit(actorId, "CREATE_USER", "User", user.getId().toString(),
                "Created user with role " + user.getRole());

        return toResponse(user);
    }

    @Transactional
    public UserResponse updateRole(UUID userId, UpdateUserRoleRequest request, UUID actorId) {
        User user = findOrThrow(userId);
        String previous = user.getRole().name();

        user.setRole(request.getRole());
        userRepository.save(user);

        audit(actorId, "UPDATE_USER_ROLE", "User", userId.toString(),
                previous + " -> " + request.getRole().name());

        return toResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(UUID userId, UpdateUserStatusRequest request, UUID actorId) {
        User user = findOrThrow(userId);
        String previous = user.getStatus().name();

        user.setStatus(request.getStatus());
        userRepository.save(user);

        audit(actorId, "UPDATE_USER_STATUS", "User", userId.toString(),
                previous + " -> " + request.getStatus().name());

        return toResponse(user);
    }

    // Helpers
    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void audit(UUID actorId, String action, String entityType,
                       String entityId, String details) {
        User actor = userRepository.findById(actorId).orElse(null);
        auditLogRepository.save(AuditLog.builder()
                .actorId(actorId)
                .actorEmail(actor != null ? actor.getEmail() : "unknown")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
