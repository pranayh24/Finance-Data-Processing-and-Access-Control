package com.finance.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendResponse {
    private String period;        // e.g. "2024-03" for monthly, "2024-W12" for weekly
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal net;
}
