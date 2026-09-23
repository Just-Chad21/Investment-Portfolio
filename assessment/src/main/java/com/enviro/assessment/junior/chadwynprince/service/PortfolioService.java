package com.enviro.assessment.junior.chadwynprince.service;

import com.enviro.assessment.junior.chadwynprince.dto.response.PortfolioResponse;
import com.enviro.assessment.junior.chadwynprince.exception.InvestorNotFoundException;
import com.enviro.assessment.junior.chadwynprince.mapper.PortfolioMapper;
import com.enviro.assessment.junior.chadwynprince.repository.PortfolioRepository;
import com.enviro.assessment.junior.chadwynprince.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final ProductRepository productRepository;
    private final PortfolioMapper portfolioMapper;

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(Long investorId) {
        var portfolio = portfolioRepository.findByInvestorId(investorId)
                .orElseThrow(() -> new InvestorNotFoundException(investorId));
        var products = productRepository.findByPortfolioId(portfolio.getId());
        return portfolioMapper.toResponse(portfolio, products);
    }
}
