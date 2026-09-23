package com.enviro.assessment.junior.chadwynprince.dto.response;

import com.enviro.assessment.junior.chadwynprince.entity.ProductType;

import java.math.BigDecimal;

public record ProductSummary(Long productId, String name, ProductType productType, BigDecimal balance) {
}
