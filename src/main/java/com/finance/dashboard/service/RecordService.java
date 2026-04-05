package com.finance.dashboard.service;

import com.finance.dashboard.dto.CreateRecordRequest;
import com.finance.dashboard.dto.UpdateRecordRequest;
import com.finance.dashboard.dto.PagedResponse;
import com.finance.dashboard.dto.RecordResponse;
import com.finance.dashboard.entity.AuditLog;
import com.finance.dashboard.entity.FinancialRecord;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.RecordType;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.repository.AuditLogRepository;
import com.finance.dashboard.repository.FinancialRecordRepository;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final FinancialRecordRepository recordRepository;
    private final UserRepository            userRepository;
    private final AuditLogRepository        auditLogRepository;

    @Transactional(readOnly = true)
    public PagedResponse<RecordResponse> getRecords(
            RecordType type, String category,
            LocalDate from, LocalDate to,
            int page, int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100)); // cap page size

        boolean noFilters = type == null
                && (category == null || category.isBlank())
                && from == null
                && to == null;

        Page<FinancialRecord> records = noFilters
                ? recordRepository.findAllActive(pageable)
                : recordRepository.findAllFiltered(
                        type != null ? type.name() : null,
                        category, from, to, pageable);

        return PagedResponse.from(records.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public RecordResponse getById(UUID id) {
        return toResponse(findActiveOrThrow(id));
    }

    @Transactional
    public RecordResponse create(CreateRecordRequest request, UUID actorId) {
        User actor = findUserOrThrow(actorId);

        FinancialRecord record = FinancialRecord.builder()
                .createdBy(actor)
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory().trim())
                .recordDate(request.getRecordDate())
                .notes(request.getNotes())
                .build();

        record = recordRepository.save(record);
        audit(actorId, "CREATE_RECORD", record.getId().toString(),
                request.getType() + " of " + request.getAmount());

        return toResponse(record);
    }

    @Transactional
    public RecordResponse update(UUID id, UpdateRecordRequest request, UUID actorId) {
        FinancialRecord record = findActiveOrThrow(id);

        // Patch — only update fields that were provided
        if (request.getAmount()     != null) record.setAmount(request.getAmount());
        if (request.getType()       != null) record.setType(request.getType());
        if (request.getCategory()   != null) record.setCategory(request.getCategory().trim());
        if (request.getRecordDate() != null) record.setRecordDate(request.getRecordDate());
        if (request.getNotes()      != null) record.setNotes(request.getNotes());

        record = recordRepository.save(record);
        audit(actorId, "UPDATE_RECORD", id.toString(), "Record updated");

        return toResponse(record);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        FinancialRecord record = findActiveOrThrow(id);
        record.setDeleted(true);           // Soft delete — never lose financial data
        recordRepository.save(record);
        audit(actorId, "DELETE_RECORD", id.toString(), "Record soft-deleted");
    }

    // Helpers
    private FinancialRecord findActiveOrThrow(UUID id) {
        return recordRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialRecord", id));
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void audit(UUID actorId, String action, String entityId, String details) {
        userRepository.findById(actorId).ifPresent(actor ->
                auditLogRepository.save(AuditLog.builder()
                        .actorId(actorId)
                        .actorEmail(actor.getEmail())
                        .action(action)
                        .entityType("FinancialRecord")
                        .entityId(entityId)
                        .details(details)
                        .build())
        );
    }

    public RecordResponse toResponse(FinancialRecord r) {
        return RecordResponse.builder()
                .id(r.getId())
                .amount(r.getAmount())
                .type(r.getType())
                .category(r.getCategory())
                .recordDate(r.getRecordDate())
                .notes(r.getNotes())
                .createdById(r.getCreatedBy().getId())
                .createdByName(r.getCreatedBy().getName())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
