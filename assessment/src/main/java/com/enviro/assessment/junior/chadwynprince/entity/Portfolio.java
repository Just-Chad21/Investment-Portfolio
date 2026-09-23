package com.enviro.assessment.junior.chadwynprince.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "investor_id", nullable = false, unique = true)
    private Investor investor;

    @Column(name = "portfolio_number", nullable = false, unique = true)
    private String portfolioNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Portfolio(Investor investor, String portfolioNumber) {
        this.investor = investor;
        this.portfolioNumber = portfolioNumber;
        this.createdAt = LocalDateTime.now();
    }
}
