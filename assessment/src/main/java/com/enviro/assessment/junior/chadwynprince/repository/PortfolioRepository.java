package com.enviro.assessment.junior.chadwynprince.repository;

import com.enviro.assessment.junior.chadwynprince.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    @Query("SELECT p FROM Portfolio p JOIN FETCH p.investor WHERE p.investor.id = :investorId")
    Optional<Portfolio> findByInvestorId(@Param("investorId") Long investorId);
}
