package com.enviro.assessment.junior.chadwynprince.repository;

import com.enviro.assessment.junior.chadwynprince.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByPortfolioId(Long portfolioId);

    // Scopes the lookup to the owning investor so a POST for a product that exists but
    // belongs to someone else's portfolio is indistinguishable from "not found".
    Optional<Product> findByIdAndPortfolio_Investor_Id(Long id, Long investorId);
}
