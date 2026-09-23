package com.enviro.assessment.junior.chadwynprince.repository;

import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalNotice;
import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalStatus;
import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WithdrawalNoticeRepository extends JpaRepository<WithdrawalNotice, Long> {

    // Each filter is applied only when its parameter is non-null — a simple, explainable
    // way to support optional filters without pulling in the Specification API for four flags.
    @Query("""
            SELECT w FROM WithdrawalNotice w
            WHERE w.investorId = :investorId
              AND (:type IS NULL OR w.type = :type)
              AND (:status IS NULL OR w.status = :status)
              AND (:from IS NULL OR w.requestedAt >= :from)
              AND (:to IS NULL OR w.requestedAt < :to)
            ORDER BY w.requestedAt DESC
            """)
    List<WithdrawalNotice> findByFilters(@Param("investorId") Long investorId,
                                          @Param("type") WithdrawalType type,
                                          @Param("status") WithdrawalStatus status,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
}
