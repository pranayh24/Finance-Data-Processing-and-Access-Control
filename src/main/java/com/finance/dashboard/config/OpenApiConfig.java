package com.finance.dashboard.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Dashboard API")
                        .description("""
                                Backend for a role-based finance dashboard system.
                                
                                **Roles:**
                                - `VIEWER` — read financial records
                                - `ANALYST` — read records + access dashboard summaries
                                - `ADMIN` — full access including user management
                                
                                **Seeded credentials (from V2 migration):**
                                - Admin:   admin@finance.com / Admin@1234
                                - Analyst: analyst@finance.com / Analyst@1234
                                - Viewer:  viewer@finance.com / Viewer@1234
                                
                                Authenticate via `POST /api/auth/login`, then click **Authorize** and paste the token.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Finance Dashboard")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT token here (without 'Bearer' prefix)")
                        ));
    }
}