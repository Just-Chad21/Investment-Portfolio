package com.enviro.assessment.junior.chadwynprince.dto.response;

import java.util.List;

public record PortfolioResponse(
        Long investorId,
        String firstName,
        String lastName,
        int age,
        String portfolioNumber,
        List<ProductSummary> products,
        WithdrawalRules withdrawalRules
) {
}
