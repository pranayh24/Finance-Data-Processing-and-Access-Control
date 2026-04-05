package com.finance.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dashboard.dto.CreateRecordRequest;
import com.finance.dashboard.dto.PagedResponse;
import com.finance.dashboard.dto.RecordResponse;
import com.finance.dashboard.enums.RecordType;
import com.finance.dashboard.security.CustomUserDetailsService;
import com.finance.dashboard.security.JwtTokenProvider;
import com.finance.dashboard.service.RecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecordController.class)
@Import(com.finance.dashboard.config.SecurityConfig.class)
class RecordControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RecordService recordService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private PagedResponse<RecordResponse> samplePage() {
        RecordResponse record = RecordResponse.builder()
                .id(UUID.randomUUID()).amount(new BigDecimal("85000.00"))
                .type(RecordType.INCOME).category("Salary")
                .recordDate(LocalDate.of(2026, 1, 1))
                .createdById(UUID.randomUUID()).createdByName("Admin")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        return PagedResponse.<RecordResponse>builder()
                .content(List.of(record))
                .page(0).size(20).totalElements(1).totalPages(1).last(true)
                .build();
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCanListRecords() throws Exception {
        when(recordService.getRecords(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(samplePage());

        mockMvc.perform(get("/api/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("Salary"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCannotCreateRecord() throws Exception {
        CreateRecordRequest req = new CreateRecordRequest();
        req.setAmount(new BigDecimal("100.00"));
        req.setType(RecordType.INCOME);
        req.setCategory("Test");
        req.setRecordDate(LocalDate.now());

        mockMvc.perform(post("/api/records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", roles = "ADMIN")
    void adminCanCreateRecord() throws Exception {
        CreateRecordRequest req = new CreateRecordRequest();
        req.setAmount(new BigDecimal("5000.00"));
        req.setType(RecordType.EXPENSE);
        req.setCategory("Travel");
        req.setRecordDate(LocalDate.of(2026, 3, 15));

        when(recordService.create(any(), any())).thenReturn(samplePage().getContent().get(0));

        mockMvc.perform(post("/api/records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", roles = "ADMIN")
    void adminCanDeleteRecord() throws Exception {
        doNothing().when(recordService).delete(any(), any());

        mockMvc.perform(delete("/api/records/" + UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
