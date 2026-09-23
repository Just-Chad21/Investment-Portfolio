package com.enviro.assessment.junior.chadwynprince.repository;

import com.enviro.assessment.junior.chadwynprince.entity.Investor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvestorRepository extends JpaRepository<Investor, Long> {

    Optional<Investor> findByEmailIgnoreCase(String email);
}
