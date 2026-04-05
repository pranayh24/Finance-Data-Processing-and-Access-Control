package com.finance.dashboard.dto;

import com.finance.dashboard.enums.RecordType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummaryResponse {
    private String category;
    private RecordType type;
    private BigDecimal total;
    private long count;
}
