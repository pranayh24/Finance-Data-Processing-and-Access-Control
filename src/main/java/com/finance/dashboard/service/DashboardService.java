package com.finance.dashboard.service;

import com.finance.dashboard.dto.CategorySummaryResponse;
import com.finance.dashboard.dto.DashboardSummaryResponse;
import com.finance.dashboard.dto.RecordResponse;
import com.finance.dashboard.dto.TrendResponse;
import com.finance.dashboard.enums.RecordType;
import com.finance.dashboard.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FinancialRecordRepository recordRepository;
    private final RecordService recordService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        BigDecimal totalIncome   = recordRepository.sumByType(RecordType.INCOME);
        BigDecimal totalExpenses = recordRepository.sumByType(RecordType.EXPENSE);
        BigDecimal net           = totalIncome.subtract(totalExpenses);

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(net)
                .totalRecords(recordRepository.countActive())
                .incomeCount(recordRepository.countByType(RecordType.INCOME))
                .expenseCount(recordRepository.countByType(RecordType.EXPENSE))
                .build();
    }

    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> getCategorySummary() {
        return recordRepository.getCategorySummary();
    }

    @Transactional(readOnly = true)
    public List<TrendResponse> getMonthlyTrends() {
        // Native query returns Object[] rows: [period, income, expenses, net]
        return recordRepository.getMonthlyTrendsRaw()
                .stream()
                .map(row -> TrendResponse.builder()
                        .period(row[0].toString())
                        .income(new BigDecimal(row[1].toString()))
                        .expenses(new BigDecimal(row[2].toString()))
                        .net(new BigDecimal(row[3].toString()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecordResponse> getRecentActivity(int limit) {
        int safeLimit = Math.min(limit, 50);
        return recordRepository
                .findRecentRecords(PageRequest.of(0, safeLimit))
                .stream()
                .map(recordService::toResponse)
                .toList();
    }
}
