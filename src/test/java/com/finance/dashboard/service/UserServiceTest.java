package com.finance.dashboard.service;

import com.finance.dashboard.dto.*;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import com.finance.dashboard.repository.AuditLogRepository;
import com.finance.dashboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private User adminUser;
    private User viewerUser;
    private final UUID adminId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private final UUID viewerId = UUID.fromString("a0000000-0000-0000-0000-000000000003");

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(adminId).name("Admin").email("admin@finance.com")
                .passwordHash("hash").role(Role.ADMIN).status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).build();

        viewerUser = User.builder()
                .id(viewerId).name("Viewer").email("viewer@finance.com")
                .passwordHash("hash").role(Role.VIEWER).status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    void createUserShouldPersistAndAudit() {
        RegisterRequest req = new RegisterRequest();
        req.setName("New Analyst");
        req.setEmail("analyst2@finance.com");
        req.setPassword("Strong@1234");
        req.setRole(Role.ANALYST);

        when(userRepository.existsByEmail("analyst2@finance.com")).thenReturn(false);
        when(passwordEncoder.encode("Strong@1234")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(LocalDateTime.now());
            return u;
        });
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        UserResponse result = userService.createUser(req, adminId);

        assertThat(result.getName()).isEqualTo("New Analyst");
        assertThat(result.getRole()).isEqualTo(Role.ANALYST);
        verify(auditLogRepository).save(argThat(a -> a.getAction().equals("CREATE_USER")));
    }

    @Test
    void updateRoleShouldChangeRoleAndAudit() {
        UpdateUserRoleRequest req = new UpdateUserRoleRequest();
        req.setRole(Role.ANALYST);

        when(userRepository.findById(viewerId)).thenReturn(Optional.of(viewerUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        UserResponse result = userService.updateRole(viewerId, req, adminId);

        assertThat(result.getRole()).isEqualTo(Role.ANALYST);
        verify(auditLogRepository).save(argThat(a -> a.getAction().equals("UPDATE_USER_ROLE")));
    }
}
