package com.finance.dashboard.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final UUID userId = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        jwtTokenProvider = new JwtTokenProvider(secret, 86400000L);
    }

    @Test
    void shouldGenerateTokenAndExtractClaims() {
        String token = jwtTokenProvider.generateToken(userId, "admin@finance.com", "ADMIN");

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getRole(token)).isEqualTo("ADMIN");

        Claims claims = jwtTokenProvider.parseToken(token);
        assertThat(claims.get("email", String.class)).isEqualTo("admin@finance.com");
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtTokenProvider.generateToken(userId, "admin@finance.com", "ADMIN");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }
}
