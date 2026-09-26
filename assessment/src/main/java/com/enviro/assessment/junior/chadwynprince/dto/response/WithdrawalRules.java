package com.enviro.assessment.junior.chadwynprince.dto.response;

import java.math.BigDecimal;

// Exposed so the frontend can show the same limits as hints without hardcoding copies
// of them; WithdrawalService still enforces them on every request.
public record WithdrawalRules(
        int minRetirementAge,
        BigDecimal maxWithdrawalRatio
) {
}
