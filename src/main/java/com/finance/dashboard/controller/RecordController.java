package com.finance.dashboard.controller;

import com.finance.dashboard.dto.CreateRecordRequest;
import com.finance.dashboard.dto.UpdateRecordRequest;
import com.finance.dashboard.dto.PagedResponse;
import com.finance.dashboard.dto.RecordResponse;
import com.finance.dashboard.enums.RecordType;
import com.finance.dashboard.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Tag(name = "Financial Records", description = "Create, view, update, and delete financial entries")
public class RecordController {

    private final RecordService recordService;

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Operation(summary = "List financial records",
            description = "Supports filtering by type, category, and date range. Results are paginated.")
    public PagedResponse<RecordResponse> getRecords(
            @Parameter(description = "Filter by type: INCOME or EXPENSE")
            @RequestParam(required = false) RecordType type,

            @Parameter(description = "Filter by category (case-insensitive)")
            @RequestParam(required = false) String category,

            @Parameter(description = "Start date (inclusive), format: yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End date (inclusive), format: yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return recordService.getRecords(type, category, from, to, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Operation(summary = "Get a single financial record by ID")
    public RecordResponse getById(@PathVariable UUID id) {
        return recordService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new financial record", description = "Admin only.")
    public RecordResponse create(
            @Valid @RequestBody CreateRecordRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return recordService.create(request, UUID.fromString(principal.getUsername()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a financial record (partial update)", description = "Admin only. Only provided fields are updated.")
    public RecordResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecordRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return recordService.update(id, request, UUID.fromString(principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a financial record", description = "Admin only. Record is marked deleted, not removed from the database.")
    public void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        recordService.delete(id, UUID.fromString(principal.getUsername()));
    }
}
