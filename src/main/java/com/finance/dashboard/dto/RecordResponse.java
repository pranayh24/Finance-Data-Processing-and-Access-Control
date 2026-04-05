package com.finance.dashboard.dto;

import com.finance.dashboard.enums.RecordType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RecordResponse {
    private UUID id;
    private BigDecimal amount;
    private RecordType type;
    private String category;
    private LocalDate recordDate;
    private String notes;
    private UUID createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
