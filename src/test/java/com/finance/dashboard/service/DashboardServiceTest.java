package com.finance.dashboard.service;

import com.finance.dashboard.dto.DashboardSummaryResponse;
import com.finance.dashboard.enums.RecordType;
import com.finance.dashboard.repository.FinancialRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private FinancialRecordRepository recordRepository;
    @Mock private RecordService recordService;

    @InjectMocks private DashboardService dashboardService;

    @Test
    void getSummaryShouldCalculateNetBalance() {
        when(recordRepository.sumByType(RecordType.INCOME)).thenReturn(new BigDecimal("272500.00"));
        when(recordRepository.sumByType(RecordType.EXPENSE)).thenReturn(new BigDecimal("78300.00"));
        when(recordRepository.countActive()).thenReturn(17L);
        when(recordRepository.countByType(RecordType.INCOME)).thenReturn(6L);
        when(recordRepository.countByType(RecordType.EXPENSE)).thenReturn(11L);

        DashboardSummaryResponse result = dashboardService.getSummary();

        assertThat(result.getTotalIncome()).isEqualByComparingTo("272500.00");
        assertThat(result.getTotalExpenses()).isEqualByComparingTo("78300.00");
        assertThat(result.getNetBalance()).isEqualByComparingTo("194200.00");
        assertThat(result.getTotalRecords()).isEqualTo(17L);
    }
}
