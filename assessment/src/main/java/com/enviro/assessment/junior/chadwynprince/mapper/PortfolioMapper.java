package com.enviro.assessment.junior.chadwynprince.mapper;

import com.enviro.assessment.junior.chadwynprince.dto.response.PortfolioResponse;
import com.enviro.assessment.junior.chadwynprince.dto.response.ProductSummary;
import com.enviro.assessment.junior.chadwynprince.entity.Portfolio;
import com.enviro.assessment.junior.chadwynprince.entity.Product;
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
                productSummaries
        );
    }

    private ProductSummary toProductSummary(Product product) {
        return new ProductSummary(product.getId(), product.getName(), product.getProductType(), product.getBalance());
    }
}
