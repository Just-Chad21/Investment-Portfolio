package com.enviro.assessment.junior.chadwynprince.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * No `type` field — whether a withdrawal is subject to the retirement age rule is a
 * property of the product being withdrawn from (Product.productType), not something the
 * client asserts. See WithdrawalService.evaluateRules.
 */
public record WithdrawalRequest(
        @NotNull Long productId,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount
) {
}
