package com.finance.dashboard.repository;

import com.finance.dashboard.dto.CategorySummaryResponse;
import com.finance.dashboard.entity.FinancialRecord;
import com.finance.dashboard.enums.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, UUID> {

    // Core filtered listing
    @Query("""
            SELECT r FROM FinancialRecord r
            WHERE r.deleted = false
              AND (:type     IS NULL OR r.type     = :type)
              AND (:category IS NULL OR LOWER(r.category) = LOWER(:category))
              AND (:from     IS NULL OR r.recordDate >= :from)
              AND (:to       IS NULL OR r.recordDate <= :to)
            ORDER BY r.recordDate DESC, r.createdAt DESC
            """)
    Page<FinancialRecord> findAllFiltered(
            @Param("type") RecordType type,
            @Param("category") String category,
            @Param("from") LocalDate from,
            @Param("to")       LocalDate to,
            Pageable pageable
    );

    // Find one (excluding soft-deleted)
    @Query("SELECT r FROM FinancialRecord r WHERE r.id = :id AND r.deleted = false")
    Optional<FinancialRecord> findActiveById(@Param("id") UUID id);

    // Dashboard: totals
    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM FinancialRecord r
            WHERE r.deleted = false AND r.type = :type
            """)
    BigDecimal sumByType(@Param("type") RecordType type);

    @Query("SELECT COUNT(r) FROM FinancialRecord r WHERE r.deleted = false AND r.type = :type")
    long countByType(@Param("type") RecordType type);

    @Query("SELECT COUNT(r) FROM FinancialRecord r WHERE r.deleted = false")
    long countActive();

    // Dashboard: by-category breakdown
    @Query("""
            SELECT new com.finance.dashboard.dto.CategorySummaryResponse(
                r.category, r.type, SUM(r.amount), COUNT(r)
            )
            FROM FinancialRecord r
            WHERE r.deleted = false
            GROUP BY r.category, r.type
            ORDER BY SUM(r.amount) DESC
            """)
    List<CategorySummaryResponse> getCategorySummary();

    // Dashboard: monthly trends (last N months)
    @Query(value = """
            SELECT
                TO_CHAR(DATE_TRUNC('month', record_date), 'YYYY-MM') AS period,
                COALESCE(SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END), 0) AS income,
                COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expenses,
                COALESCE(SUM(CASE WHEN type = 'INCOME'  THEN amount
                                  WHEN type = 'EXPENSE' THEN -amount ELSE 0 END), 0) AS net
            FROM financial_records
            WHERE is_deleted = false
              AND record_date >= DATE_TRUNC('month', NOW()) - INTERVAL '5 months'
            GROUP BY DATE_TRUNC('month', record_date)
            ORDER BY DATE_TRUNC('month', record_date)
            """, nativeQuery = true)
    List<Object[]> getMonthlyTrendsRaw();

    // Dashboard: recent activity
    @Query("""
            SELECT r FROM FinancialRecord r
            WHERE r.deleted = false
            ORDER BY r.createdAt DESC
            """)
    List<FinancialRecord> findRecentRecords(Pageable pageable);
}
