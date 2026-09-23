package com.enviro.assessment.junior.chadwynprince.dto.response;

import java.math.BigDecimal;

public record ProductSummary(Long productId, String name, BigDecimal balance) {
}
