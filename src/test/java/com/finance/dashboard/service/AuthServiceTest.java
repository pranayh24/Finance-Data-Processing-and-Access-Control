package com.finance.dashboard.service;

import com.finance.dashboard.dto.AuthResponse;
import com.finance.dashboard.dto.LoginRequest;
import com.finance.dashboard.dto.RegisterRequest;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import com.finance.dashboard.exception.ConflictException;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authManager;

    @InjectMocks private AuthService authService;

    private User sampleUser;
    private final UUID userId = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(userId).name("Admin").email("admin@finance.com")
                .passwordHash("$2a$12$encodedHash").role(Role.ADMIN)
                .status(UserStatus.ACTIVE).createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void registerShouldReturnTokenAndSaveUser() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Admin");
        req.setEmail("Admin@Finance.com"); // mixed case — should be normalized
        req.setPassword("Admin@1234");
        req.setRole(Role.ADMIN);

        when(userRepository.existsByEmail("admin@finance.com")).thenReturn(false);
        when(passwordEncoder.encode("Admin@1234")).thenReturn("$2a$12$encodedHash");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtTokenProvider.generateToken(eq(userId), eq("admin@finance.com"), eq("ADMIN")))
                .thenReturn("jwt-token");

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo("admin@finance.com");
        verify(userRepository).save(argThat(u -> u.getEmail().equals("admin@finance.com")));
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Dup");
        req.setEmail("admin@finance.com");
        req.setPassword("Pass@1234");
        req.setRole(Role.VIEWER);

        when(userRepository.existsByEmail("admin@finance.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginShouldAuthenticateAndReturnToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@finance.com");
        req.setPassword("Admin@1234");

        var principal = new org.springframework.security.core.userdetails.User(
                userId.toString(), "hash", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateToken(userId, "admin@finance.com", "ADMIN")).thenReturn("jwt-token");

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getName()).isEqualTo("Admin");
    }
}
