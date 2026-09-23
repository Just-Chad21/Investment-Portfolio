package com.enviro.assessment.junior.chadwynprince.controller;

import com.enviro.assessment.junior.chadwynprince.dto.response.PortfolioResponse;
import com.enviro.assessment.junior.chadwynprince.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/investors/{investorId}")
@RequiredArgsConstructor
public class InvestorController {

    private final PortfolioService portfolioService;

    @GetMapping("/portfolio")
    public PortfolioResponse getPortfolio(@PathVariable Long investorId) {
        return portfolioService.getPortfolio(investorId);
    }
}
