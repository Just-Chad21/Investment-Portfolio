package com.enviro.assessment.junior.chadwynprince.dto.response;

import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalStatus;
import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WithdrawalResponse(
        Long id,
        Long productId,
        String productName,
        WithdrawalType type,
        BigDecimal amount,
        WithdrawalStatus status,
        String rejectionReason,
        BigDecimal balanceAfter,
        LocalDateTime requestedAt
) {
}
