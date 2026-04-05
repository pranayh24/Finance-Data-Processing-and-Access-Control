package com.finance.dashboard.controller;

import com.finance.dashboard.dto.CategorySummaryResponse;
import com.finance.dashboard.dto.DashboardSummaryResponse;
import com.finance.dashboard.dto.RecordResponse;
import com.finance.dashboard.dto.TrendResponse;
import com.finance.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregated analytics — requires ANALYST or ADMIN role")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(
            summary = "Overall financial summary",
            description = "Returns total income, total expenses, net balance, and record counts."
    )
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/by-category")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(
            summary = "Breakdown by category",
            description = "Total amount and count grouped by category and type (INCOME/EXPENSE)."
    )
    public List<CategorySummaryResponse> getCategorySummary() {
        return dashboardService.getCategorySummary();
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(
            summary = "Monthly trends for the last 6 months",
            description = "Income, expenses, and net balance grouped by month."
    )
    public List<TrendResponse> getMonthlyTrends() {
        return dashboardService.getMonthlyTrends();
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(
            summary = "Recent financial activity",
            description = "Returns the N most recently created records. Max 50."
    )
    public List<RecordResponse> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit) {
        return dashboardService.getRecentActivity(limit);
    }
}
