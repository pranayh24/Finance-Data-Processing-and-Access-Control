package com.finance.dashboard.service;

import com.finance.dashboard.dto.*;
import com.finance.dashboard.entity.FinancialRecord;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.RecordType;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import com.finance.dashboard.repository.AuditLogRepository;
import com.finance.dashboard.repository.FinancialRecordRepository;
import com.finance.dashboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

    @Mock private FinancialRecordRepository recordRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private RecordService recordService;

    private User adminUser;
    private FinancialRecord sampleRecord;
    private final UUID userId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private final UUID recordId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(userId).name("Admin").email("admin@finance.com")
                .role(Role.ADMIN).status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).build();

        sampleRecord = FinancialRecord.builder()
                .id(recordId).createdBy(adminUser)
                .amount(new BigDecimal("85000.00")).type(RecordType.INCOME)
                .category("Salary").recordDate(LocalDate.of(2026, 1, 1))
                .notes("January salary").deleted(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getRecordsWithNoFiltersShouldUseFindAllActive() {
        var page = new PageImpl<>(List.of(sampleRecord));
        when(recordRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        PagedResponse<RecordResponse> result = recordService.getRecords(null, null, null, null, 0, 15);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategory()).isEqualTo("Salary");
        verify(recordRepository).findAllActive(any(Pageable.class));
        verify(recordRepository, never()).findAllFiltered(any(), any(), any(), any(), any());
    }

    @Test
    void getRecordsWithFilterShouldUseFindAllFiltered() {
        var page = new PageImpl<>(List.of(sampleRecord));
        when(recordRepository.findAllFiltered(eq("INCOME"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        recordService.getRecords(RecordType.INCOME, null, null, null, 0, 15);

        verify(recordRepository).findAllFiltered(eq("INCOME"), isNull(), isNull(), isNull(), any());
    }

    @Test
    void createShouldPersistRecordAndAudit() {
        CreateRecordRequest req = new CreateRecordRequest();
        req.setAmount(new BigDecimal("5000.00"));
        req.setType(RecordType.EXPENSE);
        req.setCategory("Travel");
        req.setRecordDate(LocalDate.of(2026, 3, 15));

        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));
        when(recordRepository.save(any(FinancialRecord.class))).thenAnswer(inv -> {
            FinancialRecord r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        RecordResponse result = recordService.create(req, userId);

        assertThat(result.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(result.getType()).isEqualTo(RecordType.EXPENSE);
        verify(auditLogRepository).save(argThat(a -> a.getAction().equals("CREATE_RECORD")));
    }

    @Test
    void deleteShouldSoftDeleteAndAudit() {
        when(recordRepository.findActiveById(recordId)).thenReturn(Optional.of(sampleRecord));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));

        recordService.delete(recordId, userId);

        assertThat(sampleRecord.isDeleted()).isTrue();
        verify(auditLogRepository).save(argThat(a -> a.getAction().equals("DELETE_RECORD")));
    }
}
