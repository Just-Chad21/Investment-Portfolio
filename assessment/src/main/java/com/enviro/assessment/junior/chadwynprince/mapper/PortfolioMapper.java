package com.enviro.assessment.junior.chadwynprince.mapper;

import com.enviro.assessment.junior.chadwynprince.dto.response.PortfolioResponse;
import com.enviro.assessment.junior.chadwynprince.dto.response.ProductSummary;
import com.enviro.assessment.junior.chadwynprince.dto.response.WithdrawalRules;
import com.enviro.assessment.junior.chadwynprince.entity.Portfolio;
import com.enviro.assessment.junior.chadwynprince.entity.Product;
import com.enviro.assessment.junior.chadwynprince.service.WithdrawalService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PortfolioMapper {

    public PortfolioResponse toResponse(Portfolio portfolio, List<Product> products) {
        var investor = portfolio.getInvestor();
        List<ProductSummary> productSummaries = products.stream()
                .map(this::toProductSummary)
                .toList();

        return new PortfolioResponse(
                investor.getId(),
                investor.getFirstName(),
                investor.getLastName(),
                investor.getAge(),
                portfolio.getPortfolioNumber(),
                productSummaries,
                new WithdrawalRules(WithdrawalService.MIN_RETIREMENT_AGE, WithdrawalService.MAX_WITHDRAWAL_RATIO)
        );
    }

    private ProductSummary toProductSummary(Product product) {
        return new ProductSummary(product.getId(), product.getName(), product.getProductType(), product.getBalance());
    }
}
