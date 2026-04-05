package com.finance.dashboard.controller;

import com.finance.dashboard.dto.DashboardSummaryResponse;
import com.finance.dashboard.security.CustomUserDetailsService;
import com.finance.dashboard.security.JwtTokenProvider;
import com.finance.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(com.finance.dashboard.config.SecurityConfig.class)
class DashboardControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DashboardService dashboardService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void analystCanAccessDashboardSummary() throws Exception {
        when(dashboardService.getSummary()).thenReturn(
                DashboardSummaryResponse.builder()
                        .totalIncome(new BigDecimal("272500"))
                        .totalExpenses(new BigDecimal("78300"))
                        .netBalance(new BigDecimal("194200"))
                        .totalRecords(17).incomeCount(6).expenseCount(11)
                        .build());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netBalance").value(194200))
                .andExpect(jsonPath("$.totalRecords").value(17));
    }
}
