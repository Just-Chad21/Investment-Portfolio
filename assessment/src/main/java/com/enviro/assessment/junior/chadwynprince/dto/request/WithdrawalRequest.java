package com.enviro.assessment.junior.chadwynprince.dto.request;

import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record WithdrawalRequest(
        @NotNull Long productId,
        @NotNull WithdrawalType type,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount
) {
}
