package com.enviro.assessment.junior.chadwynprince.service;

import com.enviro.assessment.junior.chadwynprince.dto.response.PortfolioResponse;
import com.enviro.assessment.junior.chadwynprince.dto.response.ProductSummary;
import com.enviro.assessment.junior.chadwynprince.dto.response.WithdrawalRules;
import com.enviro.assessment.junior.chadwynprince.entity.Investor;
import com.enviro.assessment.junior.chadwynprince.entity.Portfolio;
import com.enviro.assessment.junior.chadwynprince.entity.Product;
import com.enviro.assessment.junior.chadwynprince.entity.ProductType;
import com.enviro.assessment.junior.chadwynprince.exception.InvestorNotFoundException;
import com.enviro.assessment.junior.chadwynprince.mapper.PortfolioMapper;
import com.enviro.assessment.junior.chadwynprince.repository.PortfolioRepository;
import com.enviro.assessment.junior.chadwynprince.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for building the portfolio response (investor details, age, products, withdrawal rules)
 * from mocked repositories, and the not-found case.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private ProductRepository productRepository;

    private final PortfolioMapper portfolioMapper = new PortfolioMapper();

    @Test
    void returnsPortfolioWithProductsWhenInvestorExists() {
        var portfolioService = new PortfolioService(portfolioRepository, productRepository, portfolioMapper);

        var investor = new Investor("Thabo", "Nkosi", LocalDate.now().minusYears(45), "thabo@example.com");
        var portfolio = new Portfolio(investor, "PF-0001");
        var product = new Product(portfolio, "Unit Trust", ProductType.UNIT_TRUST, new BigDecimal("50000.00"));

        when(portfolioRepository.findByInvestorId(1L)).thenReturn(Optional.of(portfolio));
        when(productRepository.findByPortfolioId(portfolio.getId())).thenReturn(List.of(product));

        PortfolioResponse response = portfolioService.getPortfolio(1L);

        assertThat(response.firstName()).isEqualTo("Thabo");
        assertThat(response.age()).isEqualTo(45);
        assertThat(response.portfolioNumber()).isEqualTo("PF-0001");
        assertThat(response.products())
                .containsExactly(new ProductSummary(null, "Unit Trust", ProductType.UNIT_TRUST, new BigDecimal("50000.00")));
        assertThat(response.withdrawalRules())
                .isEqualTo(new WithdrawalRules(65, new BigDecimal("0.90")));
    }

    @Test
    void throwsInvestorNotFoundWhenNoPortfolioForInvestor() {
        var portfolioService = new PortfolioService(portfolioRepository, productRepository, portfolioMapper);

        when(portfolioRepository.findByInvestorId(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.getPortfolio(99L))
                .isInstanceOf(InvestorNotFoundException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(productRepository);
    }
}
