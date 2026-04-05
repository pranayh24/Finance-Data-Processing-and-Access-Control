package com.finance.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dashboard.dto.AuthResponse;
import com.finance.dashboard.dto.LoginRequest;
import com.finance.dashboard.dto.RegisterRequest;
import com.finance.dashboard.dto.UserResponse;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import com.finance.dashboard.security.CustomUserDetailsService;
import com.finance.dashboard.security.JwtTokenProvider;
import com.finance.dashboard.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(com.finance.dashboard.config.SecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private AuthResponse sampleAuth() {
        return AuthResponse.builder()
                .token("jwt-token-here")
                .user(UserResponse.builder()
                        .id(UUID.randomUUID()).name("Admin").email("admin@finance.com")
                        .role(Role.ADMIN).status(UserStatus.ACTIVE)
                        .createdAt(LocalDateTime.now()).build())
                .build();
    }

    @Test
    void registerShouldReturn201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Admin");
        req.setEmail("admin@finance.com");
        req.setPassword("Admin@1234");
        req.setRole(Role.ADMIN);

        when(authService.register(any())).thenReturn(sampleAuth());

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token-here"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void loginShouldReturn200() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@finance.com");
        req.setPassword("Admin@1234");

        when(authService.login(any())).thenReturn(sampleAuth());

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-here"));
    }

    @Test
    void loginWithBadCredentialsShouldReturn401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@finance.com");
        req.setPassword("wrong");

        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
